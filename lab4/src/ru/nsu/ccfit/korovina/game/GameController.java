package ru.nsu.ccfit.korovina.game;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;
import ru.nsu.ccfit.korovina.game.gameview.GameStateDesigner;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;
import ru.nsu.ccfit.korovina.game.gameview.GameView;
import ru.nsu.ccfit.korovina.utils.Pair;

import java.awt.event.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameController {
    private GameView gameView;
    private GameConfig config;
    private GameState gameState;
    private Runnable activateMenuCallback;
    private MessageManager messageManager;
    private HashMap<Integer, Runnable> keyEventProcessors;
    private Long lastMsgSendTime;
    private Long lastMsgFromMasterTime;
    private int id;
    private Pair<InetSocketAddress, Integer> master;

    public static NodeRole role;
    private static AtomicBoolean isClosed;

    public GameController(NodeRole role, int masterId, InetSocketAddress master, GameConfig config,
                          MessageManager messageManager, Runnable callback) {
        gameView = new GameView(config.getWidth(), config.getHeight());
        gameView.pack();
        gameView.setVisible(true);
        gameView.addBecomeAViewerActionListener(this::becomeAViewer);
        gameView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (iAmMaster()) {
                    GameMessage message = GameMessage.newBuilder()
                                            .setRoleChange(RoleChangeMsg.newBuilder()
                                                    .setReceiverRole(NodeRole.MASTER)
                                                    .build())
                                            .setSenderId(id)
                                            .setMsgSeq(messageManager.getSeqNum())
                                            .build();
                    if (getDeputy() != null) {
                        messageManager.sendMessage(message, getDeputy().getIpAddress(), getDeputy().getPort());
                    }
                }
                endGame();
            }
        });

        this.gameState = GameState.newBuilder()
                .setStateOrder(0)
                .addAllSnakes(new ArrayList<>())
                .addAllFoods(new ArrayList<>())
                .setPlayers(GamePlayers.newBuilder()
                        .addAllPlayers(new ArrayList<>()))
                .setConfig(config)
                .build();

        this.master = new Pair<>(master, masterId);
        this.config = config;
        this.messageManager = messageManager;
        this.activateMenuCallback = callback;
        this.lastMsgSendTime = System.currentTimeMillis();
        this.lastMsgFromMasterTime = System.currentTimeMillis();
        GameController.role = role;
        GameController.isClosed = new AtomicBoolean(false);

        this.keyEventProcessors = new HashMap<>();
        keyEventProcessors.put(KeyEvent.VK_UP, () -> {
            GameMessage message = composeSteerMsg(Direction.UP);
            if (role != NodeRole.VIEWER) {
                if (getMySnake(gameState.getSnakesList()).getHeadDirection() != Direction.DOWN) {
                    messageManager.sendMessage(message, this.master.getKey().getAddress().getHostAddress(), this.master.getKey().getPort());
                    lastMsgSendTime = System.currentTimeMillis();
                }
            }
        });

        keyEventProcessors.put(KeyEvent.VK_DOWN, () -> {
            GameMessage message = composeSteerMsg(Direction.DOWN);
            if (role != NodeRole.VIEWER) {
                if (getMySnake(gameState.getSnakesList()).getHeadDirection() != Direction.UP) {
                    messageManager.sendMessage(message, this.master.getKey().getAddress().getHostAddress(), this.master.getKey().getPort());
                    lastMsgSendTime = System.currentTimeMillis();
                }
            }
        });

        keyEventProcessors.put(KeyEvent.VK_LEFT, () -> {
            GameMessage message = composeSteerMsg(Direction.LEFT);
            if (role != NodeRole.VIEWER) {
                if (getMySnake(gameState.getSnakesList()).getHeadDirection() != Direction.RIGHT) {
                    messageManager.sendMessage(message, this.master.getKey().getAddress().getHostAddress(), this.master.getKey().getPort());
                    lastMsgSendTime = System.currentTimeMillis();
                }
            }
        });

        keyEventProcessors.put(KeyEvent.VK_RIGHT, () -> {
            GameMessage message = composeSteerMsg(Direction.RIGHT);
            if (role != NodeRole.VIEWER) {
                if (getMySnake(gameState.getSnakesList()).getHeadDirection() != Direction.LEFT) {
                    messageManager.sendMessage(message, this.master.getKey().getAddress().getHostAddress(), this.master.getKey().getPort());
                    lastMsgSendTime = System.currentTimeMillis();
                }
            }
        });

        gameView.setFocusable(true);
        gameView.requestFocusInWindow();
        gameView.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { }

            @Override
            public void keyPressed(KeyEvent e) {
                int event = e.getKeyCode();
                if ((event == KeyEvent.VK_UP) || (event == KeyEvent.VK_DOWN) || (event == KeyEvent.VK_RIGHT) || (event == KeyEvent.VK_LEFT)) {
                    keyEventProcessors.get(e.getKeyCode()).run();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) { }
        });
    }

    private class MasterActivityChecker extends TimerTask {
        @Override
        public void run() {
            if (isGameClosed()) {
                cancel();
            }
            if (role == NodeRole.DEPUTY) {
                if (System.currentTimeMillis() - lastMsgFromMasterTime > config.getNodeTimeoutMs()) {
                    System.out.println("I am a stupid stinky pig");
                    GamePlayer player = getPlayer(id);
                    setMaster(id, new InetSocketAddress(player.getIpAddress(), player.getPort()));
                    GameMessage roleChange = GameMessage.newBuilder()
                            .setRoleChange(RoleChangeMsg.newBuilder()
                                    .setSenderRole(NodeRole.MASTER)
                                    .build())
                            .setSenderId(id)
                            .setMsgSeq(messageManager.getSeqNum())
                            .build();
                    getGameState().getPlayers().getPlayersList().forEach((gamePlayer -> {
                        messageManager.sendMessage(roleChange, gamePlayer.getIpAddress(), gamePlayer.getPort());
                    }));
                }
            }

        }
    }

    private class PingSender extends TimerTask {
        @Override
        public void run() {
            if (isGameClosed()) {
                cancel();
            }
            if (System.currentTimeMillis() - lastMsgSendTime > config.getPingDelayMs()) {
                GameMessage message = GameMessage.newBuilder()
                        .setPing(PingMsg.newBuilder()
                                .build())
                        .setSenderId(id)
                        .setMsgSeq(messageManager.getSeqNum())
                        .build();
                messageManager.sendMessage(message, master.getKey().getAddress().getHostAddress(), master.getKey().getPort());
//                System.out.println("sent ping");
                lastMsgSendTime = System.currentTimeMillis();
            }
        }
    }

    private Snake getMySnake(List<Snake> snakesList) {
        Snake mySnake = null;
        for (Snake snake : snakesList) {
            if (snake.getPlayerId() == id) {
                mySnake = snake;
            }
        }
        return mySnake;
    }

    private GameMessage composeSteerMsg(Direction chosenDirection) {
        return GameMessage.newBuilder()
                .setMsgSeq(messageManager.getSeqNum())
                .setSteer(SteerMsg.newBuilder()
                        .setDirection(chosenDirection)
                        .build())
                .setSenderId(id)
                .build();
    }

    private boolean playerHasSnake(int id) {
        for (Snake snake : gameState.getSnakesList()) {
            if (snake.getPlayerId() == id) {
                return true;
            }
        }
        return false;
    }

    private class ListUpdater extends TimerTask {
        @Override
        public void run() {
            if (isGameClosed()) {
                cancel();
            }
            gameView.clearPlayersList();
            GamePlayers players = gameState.getPlayers();
            List<GamePlayer> playersList = players.getPlayersList();

            playersList.forEach((player) -> {
                if (playerHasSnake(player.getId())) {
                    gameView.addPlayer(player);
                }
            });
        }
    }

    public void runServices() {
        ListUpdater listUpdaterTask = new ListUpdater();
        Timer uTimer = new Timer(true);
        uTimer.schedule(listUpdaterTask, 0, 1000);

        gameView.gameStateDesigner = new GameStateDesigner(this);

        PingSender pingSender = new PingSender();
        Timer pTimer = new Timer(true);
        pTimer.schedule(pingSender, 0, 100);

        MasterActivityChecker masterActivityChecker = new MasterActivityChecker();
        Timer mTimer = new Timer(true);
        mTimer.schedule(masterActivityChecker, 0 , 1000);
    }

    public GamePlayer getPlayer(int id) {
        for (GamePlayer player : gameState.getPlayers().getPlayersList()) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    private GamePlayer getDeputy() {
        for (GamePlayer player : gameState.getPlayers().getPlayersList()) {
            if (player.getRole() == NodeRole.DEPUTY) {
                return player;
            }
        }
        return null;
    }

    private void becomeAViewer(ActionEvent e) {
        GameMessage roleChange = GameMessage.newBuilder()
                .setRoleChange(RoleChangeMsg.newBuilder()
                        .setSenderRole(NodeRole.VIEWER)
                        .build())
                .setSenderId(id)
                .setMsgSeq(messageManager.getSeqNum())
                .build();
        if (role != NodeRole.MASTER) {
            messageManager.sendMessage(roleChange, master.getKey().getAddress().getHostAddress(), master.getKey().getPort());
            lastMsgSendTime = System.currentTimeMillis();
        } else {
            GameMessage message = GameMessage.newBuilder()
                    .setRoleChange(RoleChangeMsg.newBuilder()
                            .setReceiverRole(NodeRole.MASTER)
                            .build())
                    .setSenderId(id)
                    .setMsgSeq(messageManager.getSeqNum())
                    .build();
            if (getDeputy() != null) {
                messageManager.sendMessage(message, getDeputy().getIpAddress(), getDeputy().getPort());
                messageManager.setClientMessageHandlers(this);
            } else {
                isClosed.set(true);
            }
        }
        role = NodeRole.VIEWER;
    }

    private void endGame() {
        isClosed.set(true);
        activateMenuCallback.run();
        gameView.dispose();
    }

    public void updateLastMsgFromMasterTime() { this.lastMsgFromMasterTime = System.currentTimeMillis() ; }

    public void updateGameState(GameState gameState) { this.gameState = gameState; }

    public void closeGameView() { gameView.dispose(); }

    public void runGameStateDesigner() { gameView.gameStateDesigner.run(); }

    public static boolean isGameClosed() { return isClosed.get(); }

    synchronized public GameView getGameView() { return gameView; }

    public static boolean iAmMaster() { return role == NodeRole.MASTER;}

    public int getMasterId() { return this.master.getValue(); }

    public InetSocketAddress getMasterAddress() { return this.master.getKey(); }

    public void setMaster(int id, InetSocketAddress address) { this.master = new Pair<>(address, id); }

    public GameState getGameState() { return this.gameState; }

    public int getPlayerId() { return id; }

    public void setPlayerId(int id) { this.id = id; }

    public static void setRole(NodeRole role) { GameController.role = role; }
}
