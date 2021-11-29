package ru.nsu.ccfit.korovina.server.messagehandle;

import me.ippolitov.fit.snakes.SnakesProto.GameMessage;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.AckMsg;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.ErrorMsg;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import ru.nsu.ccfit.korovina.game.GameController;
import ru.nsu.ccfit.korovina.messagemanagement.MessageHandler;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;
import ru.nsu.ccfit.korovina.server.Server;

public class JoinHandler implements MessageHandler {
    private Server server;
    private MessageManager messageManager;
    private GameController gameController;

    public JoinHandler(GameController gameController, Server server, MessageManager messageManager) {
        this.gameController = gameController;
        this.server = server;
        this.messageManager = messageManager;
    }

    private void sendAck(GameMessage message, String senderAddress, int senderPort) {
        GameMessage ack = GameMessage.newBuilder()
                .setAck(AckMsg.newBuilder()
                        .build())
                .setSenderId(gameController.getMasterId())
                .setReceiverId(server.getIdCounter() - 1)   // после успешного создания игрока idCounter уже увеличился на 1
                .setMsgSeq(message.getMsgSeq())
                .build();
        messageManager.sendMessage(ack, senderAddress, senderPort);
    }

    public void handleMessage(GameMessage message, String senderAddress, int senderPort) {
        String name = message.getJoin().getName();
        boolean onlyView = message.getJoin().getOnlyView();

        if (!onlyView) {
            if (server.createPlayer(name, NodeRole.NORMAL, senderAddress, senderPort)) {
                sendAck(message, senderAddress, senderPort);
                server.updateActivePlayers(server.getIdCounter() - 1);
            } else {
                GameMessage error = GameMessage.newBuilder()
                                                .setError(ErrorMsg.newBuilder()
                                                        .setErrorMessage("Not enough space on the game field")
                                                        .build())
                                                .setSenderId(gameController.getMasterId())
                                                .setMsgSeq(message.getMsgSeq())
                                                .build();
                messageManager.sendMessage(error, senderAddress, senderPort);
            }
        } else {
            server.createPlayer(name, NodeRole.VIEWER, senderAddress, senderPort);
            sendAck(message, senderAddress, senderPort);
            server.updateActivePlayers(server.getIdCounter() - 1);
        }

    }
}
