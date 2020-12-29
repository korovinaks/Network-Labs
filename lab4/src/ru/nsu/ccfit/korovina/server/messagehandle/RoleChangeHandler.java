package ru.nsu.ccfit.korovina.server.messagehandle;

import me.ippolitov.fit.snakes.SnakesProto.GameMessage;
import me.ippolitov.fit.snakes.SnakesProto.NodeRole;
import ru.nsu.ccfit.korovina.game.GameController;
import ru.nsu.ccfit.korovina.messagemanagement.MessageHandler;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;
import ru.nsu.ccfit.korovina.server.Server;

public class RoleChangeHandler implements MessageHandler {
    private Server server;
    private MessageManager messageManager;
    private GameController gameController;

    public RoleChangeHandler(GameController gameController, Server server, MessageManager messageManager) {
        this.gameController = gameController;
        this.server = server;
        this.messageManager = messageManager;
    }

    // обработчик сообщений от игроков, что они становятся вьюерами по собственному желанию
    public void handleMessage(GameMessage message, String senderAddress, int senderPort) {
        if (message.getRoleChange().getSenderRole() == NodeRole.VIEWER) {
            server.updateActivePlayers(message.getSenderId());
            server.updatePlayerAsViewer(message.getSenderId());
        }
        GameMessage ack = GameMessage.newBuilder()
                .setAck(GameMessage.AckMsg.newBuilder()
                        .build())
                .setSenderId(gameController.getMasterId())
                .setMsgSeq(message.getMsgSeq())
                .build();
        messageManager.sendMessage(ack, senderAddress, senderPort);
    }
}
