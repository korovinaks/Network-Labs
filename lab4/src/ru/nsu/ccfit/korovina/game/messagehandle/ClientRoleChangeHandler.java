package ru.nsu.ccfit.korovina.game.messagehandle;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.AckMsg;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.RoleChangeMsg;
import ru.nsu.ccfit.korovina.game.GameController;
import ru.nsu.ccfit.korovina.messagemanagement.MessageHandler;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;
import ru.nsu.ccfit.korovina.server.Server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ClientRoleChangeHandler implements MessageHandler {
    private GameController gameController;
    private MessageManager messageManager;

    public ClientRoleChangeHandler(GameController gameController, MessageManager messageManager) {
        this.gameController = gameController;
        this.messageManager = messageManager;
    }

    private int getMaxId() {
        int max = 0;
        for (GamePlayer player : gameController.getGameState().getPlayers().getPlayersList()) {
            if (player.getId() > max) {
                max = player.getId();
            }
        }
        return max;
    }

    private List<GamePlayer> updatePlayers() {
        List<GamePlayer> updatedPlayers = new ArrayList<>(gameController.getGameState().getPlayers().getPlayersList());
        updatedPlayers.forEach((gamePlayer) -> {
            if (gamePlayer.getId() == gameController.getMasterId()) {
                int index = updatedPlayers.indexOf(gamePlayer);
                updatedPlayers.set(index, GamePlayer.newBuilder()
                        .setName(gamePlayer.getName())
                        .setId(gamePlayer.getId())
                        .setIpAddress(gamePlayer.getIpAddress())
                        .setPort(gamePlayer.getPort())
                        .setRole(NodeRole.VIEWER)
                        .setScore(gamePlayer.getScore())
                        .build());
            } else if (gamePlayer.getId() == gameController.getPlayerId()) {
                int index = updatedPlayers.indexOf(gamePlayer);
                updatedPlayers.set(index, GamePlayer.newBuilder()
                        .setName(gamePlayer.getName())
                        .setId(gamePlayer.getId())
                        .setIpAddress(gamePlayer.getIpAddress())
                        .setPort(gamePlayer.getPort())
                        .setRole(NodeRole.MASTER)
                        .setScore(gamePlayer.getScore())
                        .build());
            }
        });
        System.out.println("updated players " + updatedPlayers);
        return updatedPlayers;
    }

    private GameState updateGameState(List<GamePlayer> updatedPlayers) {
        return GameState.newBuilder()
                        .setConfig(gameController.getGameState().getConfig())
                        .setStateOrder(gameController.getGameState().getStateOrder())
                        .addAllSnakes(gameController.getGameState().getSnakesList())
                        .addAllFoods(gameController.getGameState().getFoodsList())
                        .setPlayers(GamePlayers.newBuilder()
                                        .addAllPlayers(updatedPlayers)
                                        .build())
                        .build();
    }

    public void handleMessage(GameMessage message, String senderAddress, int senderPort) {
        gameController.updateLastMsgFromMasterTime();

        RoleChangeMsg receivedMessage = message.getRoleChange();
        if (receivedMessage.getSenderRole() == NodeRole.MASTER) {
            gameController.setMaster(message.getSenderId(), new InetSocketAddress(senderAddress, senderPort));
            messageManager.updateSentMessages();
        }
        if (receivedMessage.getReceiverRole() == NodeRole.MASTER) {
//            System.out.println("I am master now, my id is " + gameController.getPlayerId());
            GamePlayer player = gameController.getPlayer(gameController.getPlayerId());

            List<GamePlayer> updatedPlayers = updatePlayers();
            GameState updatedGameState = updateGameState(updatedPlayers);

            gameController.setMaster(gameController.getPlayerId(), new InetSocketAddress(player.getIpAddress(), player.getPort()));
            messageManager.updateSentMessages();
            GameController.setRole(NodeRole.MASTER);

            Server server = new Server(messageManager, gameController.getGameState().getConfig(), getMaxId() + 1);
            messageManager.setServerMessageHandlers(server, gameController);
            server.setMasterId(gameController.getPlayerId());

            server.updatePlayers(updatedPlayers);
            server.setLastGameState(updatedGameState);
            server.run();

            GameMessage roleChange = GameMessage.newBuilder()
                    .setRoleChange(RoleChangeMsg.newBuilder()
                            .setSenderRole(NodeRole.MASTER)
                            .build())
                    .setSenderId(gameController.getPlayerId())
                    .setMsgSeq(messageManager.getSeqNum())
                    .build();
            gameController.getGameState().getPlayers().getPlayersList().forEach((gamePlayer -> {
                messageManager.sendMessage(roleChange, gamePlayer.getIpAddress(), gamePlayer.getPort());
            }));
        }
        if (receivedMessage.getReceiverRole() == NodeRole.VIEWER) {
            GameController.setRole(NodeRole.VIEWER);
        }
        if (receivedMessage.getReceiverRole() == NodeRole.DEPUTY) {
            GameController.setRole(NodeRole.DEPUTY);
        }

        GameMessage ack = GameMessage.newBuilder()
                .setMsgSeq(message.getMsgSeq())
                .setAck(AckMsg.newBuilder()
                        .build())
                .setSenderId(gameController.getPlayerId())
                .build();
        messageManager.sendMessage(ack, senderAddress, senderPort);

    }
}
