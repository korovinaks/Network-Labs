package ru.nsu.ccfit.korovina.messagemanagement;

import me.ippolitov.fit.snakes.SnakesProto.*;

public interface MessageHandler {
    void handleMessage(GameMessage message, String senderAddress, int senderPort);
}
