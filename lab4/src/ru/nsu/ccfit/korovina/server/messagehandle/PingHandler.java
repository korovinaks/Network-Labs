package ru.nsu.ccfit.korovina.server.messagehandle;


import me.ippolitov.fit.snakes.SnakesProto;
import ru.nsu.ccfit.korovina.game.GameController;
import ru.nsu.ccfit.korovina.messagemanagement.MessageHandler;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;
import ru.nsu.ccfit.korovina.server.Server;

public class PingHandler implements MessageHandler {
    private Server server;
    private MessageManager messageManager;
    private GameController gameController;

    public PingHandler(GameController gameController, Server server, MessageManager messageManager) {
        this.gameController = gameController;
        this.server = server;
        this.messageManager = messageManager;
    }

    public void handleMessage(SnakesProto.GameMessage message, String senderAddress, int senderPort) {
//        System.out.println("received ping from " + message.getSenderId());
        server.updateActivePlayers(message.getSenderId());
        SnakesProto.GameMessage ack = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(message.getMsgSeq())
                .setAck(SnakesProto.GameMessage.AckMsg.newBuilder()
                        .build())
                .setSenderId(gameController.getMasterId())
                .build();
        messageManager.sendMessage(ack, senderAddress, senderPort);
    }
}
