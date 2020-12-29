package ru.nsu.ccfit.korovina.server;

import ru.nsu.ccfit.korovina.game.GameController;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameStateUpdater extends TimerTask {
    private GameState gameState;
    private GameState updatedGameState;
    private Server server;
    private List<Snake> snakes;
    private List<Snake> deadSnakes;
    private List<GamePlayer> players;
    private List<Coord> foods;
    private List<GameMessage> relevantSteers;

    public GameStateUpdater(Server server) {
        this.server = server;
    }

    private GameMessage getRelevantSteer(GameMessage currentMessage, Long currentReceiveTime, ConcurrentHashMap<GameMessage, Long> receivedSteers) {
        int currentPlayerId = currentMessage.getSenderId();
        Long latestReceiveTime = currentReceiveTime;
        GameMessage relevantMessage = currentMessage;

        GameMessage message;
        Long receiveTime;
        for (Map.Entry<GameMessage, Long> entry : receivedSteers.entrySet()) {
            message = entry.getKey();
            receiveTime = entry.getValue();
            if (message.getSenderId() == currentPlayerId) {
                if (receiveTime > latestReceiveTime) {
                    latestReceiveTime = receiveTime;
                    relevantMessage = message;
                }
            }
        }
        return relevantMessage;
    }

    private Direction getNewDirection(Snake snake) {
        Direction newDirection = snake.getHeadDirection();
        for (GameMessage message : relevantSteers) {
            if (message.getSenderId() == snake.getPlayerId()) {
                newDirection = message.getSteer().getDirection();
            }
        }
        return newDirection;
    }

    private boolean crashedIntoSnake(Coord head, int id) {
        for (Snake snake : snakes) {
            List<Coord> snakePoints = snake.getPointsList();
            List<Coord> absoluteOtherSnakeCoords = getSnakesAbsoluteCoords(snakePoints);
            if (snake.getPlayerId() == id) {
                absoluteOtherSnakeCoords.remove(0);
            }
            if (absoluteOtherSnakeCoords.contains(head)) {
                return true;
            }
        }
        return false;
    }

    private Coord getFood(Coord point) {
        if (foods.contains(point)) {
            return point;
        }
        return null;
    }

    private Coord getNewHeadCoords(Snake snake, Direction headDirection) {
        Coord head = snake.getPoints(0);
        int headX = head.getX();
        int headY = head.getY();

        if (headDirection == Direction.UP) {
            headY -= 1;
        } else if (headDirection == Direction.DOWN) {
            headY += 1;
        } else if (headDirection == Direction.LEFT) {
            headX -= 1;
        } else if (headDirection == Direction.RIGHT) {
            headX += 1;
        }
        return Coord.newBuilder()
                .setX((headX + server.getConfig().getWidth()) % server.getConfig().getWidth())
                .setY((headY + server.getConfig().getHeight()) % server.getConfig().getHeight())
                .build();
    }

    private Coord move(Coord coord, int dx, int dy) {
        int x = coord.getX();
        int y = coord.getY();

        return Coord.newBuilder()
                .setX(Integer.signum(x) * (Math.abs(x) + dx))
                .setY(Integer.signum(y) * (Math.abs(y) + dy))
                .build();
    }

    private List<Coord> getNewCoords(Direction oldDirection, Direction newDirection, Coord newHead, List<Coord> oldCoords, boolean ateFood) {
        List<Coord> newCoords = new ArrayList<>();
        newCoords.add(newHead);
        Coord oldHead = oldCoords.get(0);
        int start;

        if (oldDirection.equals(newDirection)) {
            if (oldCoords.get(1).getX() != 0) {
                newCoords.add(move(oldCoords.get(1), 1, 0));
            } else if (oldCoords.get(1).getY() != 0) {
                newCoords.add(move(oldCoords.get(1), 0, 1));
            }
            start = 2;
        } else {
            int newKeyPointX = oldHead.getX() - newHead.getX();
            int newKeyPointY = oldHead.getY() - newHead.getY();
            if (Math.abs(newKeyPointX) > 1) {
                newKeyPointX /= Math.abs(newKeyPointX) * (-1);
            } else if (Math.abs(newKeyPointY) > 1) {
                newKeyPointY /= Math.abs(newKeyPointY) * (-1);
            }
            newCoords.add(Coord.newBuilder()
                                        .setX(newKeyPointX)
                                        .setY(newKeyPointY)
                                        .build());
            start = 1;
        }

        for (int i = start; i < oldCoords.size(); i++) {
            newCoords.add(oldCoords.get(i));
        }

        Coord oldTail = newCoords.get(newCoords.size() - 1);
        newCoords.remove(newCoords.size() - 1);
        if (!ateFood) {
            if (Math.abs(oldTail.getX()) != 1 && Math.abs(oldTail.getY()) != 1) {
                if (oldTail.getX() != 0) {
                    newCoords.add(move(oldTail, -1, 0));
                } else if (oldTail.getY() != 0) {
                    newCoords.add(move(oldTail, 0, -1));
                }
            }
        } else {
            newCoords.add(oldTail);
        }
        return newCoords;
    }

    private Snake updateSnake(Snake snake) {
        Direction headDirection = getNewDirection(snake);
        Coord newHeadCoords = getNewHeadCoords(snake, headDirection);
        List<Coord> oldCoords = new ArrayList<>(snake.getPointsList());

        Snake updatedSnake;

        Coord eatenFood = getFood(newHeadCoords);

        if (eatenFood != null) {
            updatedSnake = Snake.newBuilder()
                    .setPlayerId(snake.getPlayerId())
                    .setHeadDirection(headDirection)
                    .setState(snake.getState())
                    .addAllPoints(getNewCoords(snake.getHeadDirection(), headDirection, newHeadCoords, oldCoords, true))
                    .build();
            GamePlayer player = getPlayer(updatedSnake.getPlayerId());
            if (player != null) {
                int index = players.indexOf(player);
                GamePlayer updatedPlayer = GamePlayer.newBuilder()
                                                .setName(player.getName())
                                                .setId(player.getId())
                                                .setIpAddress(player.getIpAddress())
                                                .setPort(player.getPort())
                                                .setRole(player.getRole())
                                                .setScore(player.getScore() + 1)
                                                .build();
                players.set(index, updatedPlayer);
            }
            foods.remove(eatenFood);
        } else {
            updatedSnake = Snake.newBuilder()
                    .setPlayerId(snake.getPlayerId())
                    .setHeadDirection(headDirection)
                    .setState(snake.getState())
                    .addAllPoints(getNewCoords(snake.getHeadDirection(), headDirection, newHeadCoords, oldCoords, false))
                    .build();

        }
        return updatedSnake;
    }

    List<Coord> getSnakesAbsoluteCoords(List<Coord> coords) {
        List<Coord> absoluteCoords = new ArrayList<>();
        int previousX = coords.get(0).getX();
        int previousY = coords.get(0).getY();

        int cellsHorizontal = server.getConfig().getWidth();
        int cellsVertical = server.getConfig().getHeight();

        int dx, dy, startX, startY, nextX, nextY;
        for (int i = 1; i < coords.size(); i++) {
            startX = previousX;
            startY = previousY;
            nextX = previousX + coords.get(i).getX();
            nextY = previousY + coords.get(i).getY();
            dx = startX > nextX ? -1 : 1;
            dy = startY > nextY ? -1 : 1;
            for (int x = startX; x != nextX + dx; x += dx) {
                for (int y = startY; y != nextY + dy; y += dy) {
                    Coord pointToAdd = Coord.newBuilder()
                            .setX((x + cellsHorizontal) % cellsHorizontal)
                            .setY((y + cellsVertical) % cellsVertical)
                            .build();
                    absoluteCoords.add(pointToAdd);
                }
            }
            previousX = (previousX + coords.get(i).getX() + cellsHorizontal) % cellsHorizontal;
            previousY = (previousY + coords.get(i).getY() + cellsVertical) % cellsVertical;
        }
        return absoluteCoords;
    }

    private void replaceDeadSnakesWithFood() {
        Random rand = new Random(System.currentTimeMillis());
        float probabilityOfBecomingFood = server.getConfig().getDeadFoodProb();
        for (Snake deadSnake : deadSnakes) {
             List<Coord> snakeAbsoluteCoords = getSnakesAbsoluteCoords(deadSnake.getPointsList());
             snakeAbsoluteCoords.forEach((coord) -> {
                 if (rand.nextFloat() <= probabilityOfBecomingFood) {
                     foods.add(coord);
                 }
             });
        }
    }

    private boolean occupiedBySnakes(Coord point) {
        for (Snake snake : snakes) {
            if (getSnakesAbsoluteCoords(snake.getPointsList()).contains(point)) {
                return true;
            }
        }
        return false;
    }

    private void dropNewFood() {
        int foodStatic = server.getConfig().getFoodStatic();
        int additionalFood = (int)(snakes.size() * server.getConfig().getFoodPerPlayer());
        int currentFoodQuantity = foods.size();

        Random rand = new Random(System.currentTimeMillis());

        Coord coord;
        int x, y;
        int cellsHorizontal = server.getConfig().getWidth();
        int cellsVertical = server.getConfig().getHeight();
        while (currentFoodQuantity < foodStatic + additionalFood) {
            x = rand.nextInt(cellsHorizontal);
            y = rand.nextInt(cellsVertical);
            coord = Coord.newBuilder()
                    .setX(x)
                    .setY(y)
                    .build();
            if (!foods.contains(coord) && !occupiedBySnakes(coord)) {
                foods.add(coord);
                currentFoodQuantity++;
            }
        }
    }

    private GamePlayer getPlayer(int id) {
        for (GamePlayer player : players) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    private void updateDeadPlayers() {
        for (Snake snake : deadSnakes) {
            GamePlayer player = getPlayer(snake.getPlayerId());
            if (player != null) {
                if (player.getRole() == NodeRole.MASTER) {
                    server.assignMaster();
                } else {
                    server.notifyDeadPlayer(snake.getPlayerId());
                }

                if (player.getRole() == NodeRole.DEPUTY) {
                    server.assignDeputy(null);
                }

                int index = players.indexOf(player);
                GamePlayer updatedPlayer = GamePlayer.newBuilder()
                        .setName(player.getName())
                        .setId(player.getId())
                        .setIpAddress(player.getIpAddress())
                        .setPort(player.getPort())
                        .setRole(NodeRole.VIEWER)
                        .setScore(player.getScore())
                        .build();
                players.set(index, updatedPlayer);
            }
        }
    }

    GameState getUpdatedGameState() { return this.updatedGameState; }

    @Override
    public void run() {
        if (!GameController.iAmMaster() || GameController.isGameClosed()) {
            cancel();
        }

        synchronized (server.getLock()) {
            gameState = server.getGameState();
            snakes = new ArrayList<>(gameState.getSnakesList());
            players = new ArrayList<>(gameState.getPlayers().getPlayersList());
            foods = new ArrayList<>(gameState.getFoodsList());
            deadSnakes = new ArrayList<>();

            ConcurrentHashMap<GameMessage, Long> receivedSteers = server.getReceivedSteers();
            relevantSteers = new ArrayList<>();
            receivedSteers.forEach((message, receiveTime) -> relevantSteers.add(getRelevantSteer(message, receiveTime, receivedSteers)));
            server.clearReceivedSteers();

            for (Snake snake : snakes) {
                int index = snakes.indexOf(snake);
                Snake updatedSnake = updateSnake(snake);
                snakes.set(index, updatedSnake);
            }

            for (Snake snake : snakes) {
                Coord head = snake.getPointsList().get(0);
                if (crashedIntoSnake(head, snake.getPlayerId())) {
                    deadSnakes.add(snake);
                }
            }

            snakes.removeIf(snake -> deadSnakes.contains(snake));

            dropNewFood();
            replaceDeadSnakesWithFood();

            updatedGameState = GameState.newBuilder()
                    .setStateOrder(server.getStateOrder())
                    .addAllSnakes(snakes)
                    .addAllFoods(foods)
                    .setPlayers(GamePlayers.newBuilder()
                            .addAllPlayers(players)
                            .build())
                    .setConfig(server.getConfig())
                    .build();
            server.setLastGameState(updatedGameState);
            server.sendGameState();
            updateDeadPlayers();
        }
    }
}
