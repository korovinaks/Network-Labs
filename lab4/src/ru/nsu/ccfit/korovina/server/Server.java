package ru.nsu.ccfit.korovina.server;

import ru.nsu.ccfit.korovina.game.GameController;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private GameState gameState;
    private final Object lock;
    private int stateOrder;
    private MessageManager messageManager;
    private GameStateUpdater gameStateUpdater;
    private AnnouncementSender announcementSender;
    private ActivePlayersChecker activePlayersChecker;
    private GameConfig config;
    private int idCounter;
    private int masterId;
    private ConcurrentHashMap<GameMessage, Long> receivedSteers;
    private ConcurrentHashMap<GamePlayer, Long> players;
    private GamePlayer deputy;

    public Server(MessageManager messageManager, GameConfig config, int idCounter) {
        this.messageManager = messageManager;
        this.stateOrder = 0;
        this.config = config;
        this.receivedSteers = new ConcurrentHashMap<>();
        this.idCounter = idCounter;
        this.lock = new Object();
        this.players = new ConcurrentHashMap<>();
        this.deputy = null;

        this.gameState = GameState.newBuilder()
                            .setStateOrder(stateOrder)
                            .addAllSnakes(new ArrayList<>())
                            .addAllFoods(new ArrayList<>())
                            .setPlayers(GamePlayers.newBuilder()
                                            .addAllPlayers(new ArrayList<>()))
                            .setConfig(config)
                            .build();
    }

    private class AnnouncementSender extends TimerTask {
        @Override
        public void run() {
            if (!GameController.iAmMaster() || GameController.isGameClosed()) {
                cancel();
            }
            GameMessage message = GameMessage.newBuilder()
                    .setAnnouncement(AnnouncementMsg.newBuilder()
                            .setPlayers(GamePlayers.newBuilder()
                                    .addAllPlayers(players.keySet())
                                    .build())
                            .setConfig(config)
                            .setCanJoin(true)
                            .build())
                    .setMsgSeq(messageManager.getSeqNum())
                    .build();
            messageManager.sendMessage(message, "239.192.0.4", 9192);
        }
    }

    private class ActivePlayersChecker extends TimerTask {
        @Override
        public void run() {
            if (!GameController.iAmMaster() || GameController.isGameClosed()) {
                cancel();
            }
            for (Map.Entry<GamePlayer, Long> player : players.entrySet()) {
                Long lastMessageTime = player.getValue();
                if (System.currentTimeMillis() - lastMessageTime > config.getNodeTimeoutMs()) {
//                    System.out.println("player " + player.getKey().getId() + " is considered as dead");
                    if (deputy != null) {
                        if (deputy.equals(player.getKey())) {
                            deputy = null;
                        }
                    }
                    players.remove(player.getKey());
                    updateSnakeAsZombie(player.getKey().getId());
                } else if (deputy == null && player.getKey().getId() != masterId && player.getKey().getRole() != NodeRole.VIEWER) {
                    assignDeputy(player.getKey());
//                    System.out.println("my deputy is " + player.getKey().getId() + ", master's id is " + masterId);
                }
            }
        }
    }

    public boolean createPlayer(String name, NodeRole role, String ipAddress, int port) {
        GamePlayer player = GamePlayer.newBuilder()
                .setName(name)
                .setId(idCounter)
                .setIpAddress(ipAddress)
                .setPort(port)
                .setRole(role)
                .setScore(0)
                .build();
        synchronized (lock) {
            if (role != NodeRole.VIEWER) {
                Snake snake = tryToDropSnake(idCounter);
                if (snake != null) {
                    System.out.println("created one more snake");
                    players.put(player, System.currentTimeMillis());
                    List<Snake> updatedSnakesList = new ArrayList<>(gameState.getSnakesList());
                    updatedSnakesList.add(snake);
                    gameState = GameState.newBuilder()
                            .setStateOrder(stateOrder)
                            .addAllSnakes(updatedSnakesList)
                            .addAllFoods(gameState.getFoodsList())
                            .setPlayers(GamePlayers.newBuilder()
                                    .addAllPlayers(players.keySet()))
                            .setConfig(gameState.getConfig())
                            .build();
                    idCounter++;
                    return true;
                }
            } else {
                players.put(player, System.currentTimeMillis());
                idCounter++;
                return true;
            }
        }
        return false;
    }

    public void run() {
        gameStateUpdater = new GameStateUpdater(this);
        Timer uTimer = new Timer(true);
        uTimer.schedule(gameStateUpdater, 0, config.getStateDelayMs());

        announcementSender = new AnnouncementSender();
        Timer aTimer = new Timer(true);
        aTimer.schedule(announcementSender, 0, 1000);

        activePlayersChecker = new ActivePlayersChecker();
        Timer cTimer = new Timer(true);
        cTimer.schedule(activePlayersChecker, 0, 100);
    }

    private List<Coord> generateSnakeCoords(int d, int headX, int headY) {
        List<Coord> snakeCoords = new ArrayList<>();

        int tailX = 0, tailY = 0;

        try {
            switch (d) {
                // направление головы вверх => хвоста - вниз
                case (0):
                    tailX = 0;
                    tailY = 1;
                    break;
                // направление головы вниз => хвоста - вверх
                case (1):
                    tailX = 0;
                    tailY = -1;
                    break;
                // направление головы влево => хвоста - вправо
                case (2):
                    tailX = 1;
                    tailY = 0;
                    break;
                // направление головы вправо => хвоста - влево
                case (3):
                    tailX = -1;
                    tailY = 0;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Value of d must be between 0 and 3");
        }

        snakeCoords.add(Coord.newBuilder()
                .setX(headX)
                .setY(headY)
                .build());
        snakeCoords.add(Coord.newBuilder()
                .setX(tailX)
                .setY(tailY)
                .build());
        return snakeCoords;
    }

    private Direction generateHeadDirection(int d) {
        Direction headDirection = null;
        try {
            switch (d) {
                case (0):
                    headDirection = Direction.UP;
                    break;
                case (1):
                    headDirection = Direction.DOWN;
                    break;
                case (2):
                    headDirection = Direction.LEFT;
                    break;
                case (3):
                    headDirection = Direction.RIGHT;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Value of d must be between 0 and 3");
        }
        return headDirection;
    }

    private Snake tryToDropSnake(int id) {
        Random rand = new Random(System.currentTimeMillis());

        int cellsHorizontal = config.getWidth();
        int cellsVertical = config.getHeight();

        boolean foundFreeSquare = true;

        List<Coord> cellsLeft = new ArrayList<>();
        for (int i = 0; i < cellsHorizontal; i++) {
            for (int j = 0; j < cellsVertical; j++) {
                cellsLeft.add(Coord.newBuilder()
                                .setX(i)
                                .setY(j)
                                .build());
            }
        }
        int index = 0, centerX = 0, centerY = 0;

        while (!cellsLeft.isEmpty()) {
            centerX = cellsLeft.get(index).getX();
            centerY = cellsLeft.get(index).getY();
            List<Coord> square = new ArrayList<>();
            for (int i = centerX - 2; i <= centerX + 2; i++) {
                for (int j = centerY - 2; j <= centerY + 2; j++) {
                    square.add(Coord.newBuilder()
                            .setX((i + cellsHorizontal) % cellsHorizontal)
                            .setY((j + cellsVertical) % cellsVertical)
                            .build());
                }
            }
            foundFreeSquare = true;

            for (Coord cell : square) {
                if (gameState.getFoodsList().contains(cell)) {
                    foundFreeSquare = false;
                }
                List<Snake> snakes = gameState.getSnakesList();
                for (Snake snake : snakes) {
                    if (gameStateUpdater.getSnakesAbsoluteCoords(snake.getPointsList()).contains(cell)) {
                        foundFreeSquare = false;
                    }
                }
            }

            if (foundFreeSquare) {
                break;
            }
            index++;
            cellsLeft.remove(Coord.newBuilder()
                                .setX(centerX)
                                .setY(centerY)
                                .build());
        }

        if (!foundFreeSquare) {
            return null;
        }
        int d = rand.nextInt(4);
        Direction headDirection = generateHeadDirection(d);
        List<Coord> snakeCoords = generateSnakeCoords(d, centerX, centerY);
        return Snake.newBuilder()
                .setPlayerId(id)
                .addAllPoints(snakeCoords)
                .setState(Snake.SnakeState.ALIVE)
                .setHeadDirection(headDirection)
                .build();
    }

    void sendGameState() {
//        System.out.println("sending game state to " + players.keySet());
        players.keySet().forEach((player -> {
            GameMessage message = GameMessage.newBuilder()
                    .setState(GameMessage.StateMsg.newBuilder()
                            .setState(gameStateUpdater.getUpdatedGameState())
                            .build())
                    .setMsgSeq(messageManager.getSeqNum())
                    .build();
            messageManager.sendMessage(message, player.getIpAddress(), player.getPort());
 //         System.out.println("sent game state to " + player.getId());
        }));
        stateOrder++;
    }

    public void updateActivePlayers(int id) {
//        System.out.println("updateActivePlayers");
        for (GamePlayer player : players.keySet()) {
//            System.out.print(player.getId() + " ");
            if (player.getId() == id) {
                players.put(player, System.currentTimeMillis());
            }
        }
//        System.out.println();
    }

    public void addSteerMsg(GameMessage message) { receivedSteers.put(message, System.currentTimeMillis()); }

    ConcurrentHashMap<GameMessage, Long> getReceivedSteers() { return this.receivedSteers; }

    GameState getGameState() { return this.gameState; }

    void clearReceivedSteers() { receivedSteers.clear(); }

    GameConfig getConfig() { return this.config; }

    public void setLastGameState(GameState gameState) { this.gameState = gameState; }

    public void updatePlayers(List<GamePlayer> players) {
        this.players.clear();
        players.forEach((player) -> {
            this.players.put(player, System.currentTimeMillis());
        });
    }

    public int getIdCounter () { return this.idCounter; }

    void notifyDeadPlayer(int playerId) {
        players.keySet().forEach((player) -> {
            if (player.getId() == playerId) {
                GameMessage roleChange = GameMessage.newBuilder()
                        .setRoleChange(RoleChangeMsg.newBuilder()
                                .setReceiverRole(NodeRole.VIEWER)
                                .build())
                        .setSenderId(masterId)
                        .setMsgSeq(messageManager.getSeqNum())
                        .build();
                messageManager.sendMessage(roleChange, player.getIpAddress(), player.getPort());
            }
        });
    }

    void assignMaster() {
        GameController.setRole(NodeRole.VIEWER);
        if (deputy != null) {
            GameMessage roleChange = GameMessage.newBuilder()
                    .setRoleChange(RoleChangeMsg.newBuilder()
                            .setReceiverRole(NodeRole.MASTER)
                            .build())
                    .setSenderId(masterId)
                    .setMsgSeq(messageManager.getSeqNum())
                    .build();
            messageManager.sendMessage(roleChange, deputy.getIpAddress(), deputy.getPort());
            messageManager.updateClientMessageHandlers();
            System.out.println("ASSIGNED NEW MASTER");
        }
    }

    void assignDeputy(GamePlayer player) {
        deputy = player;
        if (deputy != null) {
            players.keySet().forEach((currentPlayer) -> {
                if (currentPlayer.getId() == player.getId()) {
                    GamePlayer updatedPlayer = GamePlayer.newBuilder()
                            .setName(currentPlayer.getName())
                            .setId(currentPlayer.getId())
                            .setIpAddress(currentPlayer.getIpAddress())
                            .setPort(currentPlayer.getPort())
                            .setRole(NodeRole.DEPUTY)
                            .setScore(currentPlayer.getScore())
                            .build();
                    players.remove(currentPlayer);
                    players.put(updatedPlayer, System.currentTimeMillis());
                }
            });
            gameState = GameState.newBuilder()
                    .setStateOrder(gameState.getStateOrder())
                    .setConfig(gameState.getConfig())
                    .setPlayers(GamePlayers.newBuilder()
                            .addAllPlayers(players.keySet())
                            .build())
                    .addAllFoods(gameState.getFoodsList())
                    .addAllSnakes(gameState.getSnakesList())
                    .build();

            GameMessage roleChange = GameMessage.newBuilder()
                    .setRoleChange(RoleChangeMsg.newBuilder()
                            .setReceiverRole(NodeRole.DEPUTY)
                            .build())
                    .setSenderId(masterId)
                    .setMsgSeq(messageManager.getSeqNum())
                    .build();
            messageManager.sendMessage(roleChange, deputy.getIpAddress(), player.getPort());
        }

    }

    public int getStateOrder() { return this.stateOrder; }

    Object getLock() { return this.lock; }

    public void setMasterId(int masterId) { this.masterId = masterId; }

    public int getMasterId() { return masterId; }

    private void updateSnakeAsZombie(int playerId) {
        List<Snake> snakesList = new ArrayList<>(gameState.getSnakesList());
        snakesList.forEach((snake -> {
            if (snake.getPlayerId() == playerId) {
                int index = snakesList.indexOf(snake);
                Snake updatedSnake = Snake.newBuilder()
                        .setPlayerId(snake.getPlayerId())
                        .setHeadDirection(snake.getHeadDirection())
                        .addAllPoints(snake.getPointsList())
                        .setState(SnakeState.ZOMBIE)
                        .build();
                snakesList.set(index, updatedSnake);
            }
        }));
        gameState = GameState.newBuilder()
                .setStateOrder(gameState.getStateOrder())
                .setConfig(gameState.getConfig())
                .setPlayers(gameState.getPlayers())
                .addAllFoods(gameState.getFoodsList())
                .addAllSnakes(snakesList)
                .build();
    }

    // обработка ситуации, когда игрок по собственному желанию стал вьюером
    public void updatePlayerAsViewer(int playerId) {
        for (GamePlayer player : players.keySet()) {
            if (player.getId() == playerId) {
                if (player.getRole() == NodeRole.DEPUTY) {
                    assignDeputy(null);
                }
                GamePlayer updatedPlayer = GamePlayer.newBuilder()
                                                .setName(player.getName())
                                                .setId(player.getId())
                                                .setIpAddress(player.getIpAddress())
                                                .setPort(player.getPort())
                                                .setRole(NodeRole.VIEWER)
                                                .setScore(player.getScore())
                                                .build();
                players.remove(player);
                players.put(updatedPlayer, System.currentTimeMillis());
            }
        }
        updateSnakeAsZombie(playerId);
        gameState = GameState.newBuilder()
                .setStateOrder(gameState.getStateOrder())
                .setConfig(gameState.getConfig())
                .setPlayers(GamePlayers.newBuilder()
                                    .addAllPlayers(players.keySet())
                                    .build())
                .addAllFoods(gameState.getFoodsList())
                .addAllSnakes(gameState.getSnakesList())
                .build();
    }
}
