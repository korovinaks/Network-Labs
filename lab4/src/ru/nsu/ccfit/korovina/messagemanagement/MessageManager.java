package ru.nsu.ccfit.korovina.messagemanagement;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.nsu.ccfit.korovina.game.GameController;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import ru.nsu.ccfit.korovina.game.messagehandle.ClientRoleChangeHandler;
import ru.nsu.ccfit.korovina.game.messagehandle.ErrorHandler;
import ru.nsu.ccfit.korovina.game.messagehandle.StateHandler;
import ru.nsu.ccfit.korovina.server.Server;
import ru.nsu.ccfit.korovina.server.messagehandle.JoinHandler;
import ru.nsu.ccfit.korovina.server.messagehandle.PingHandler;
import ru.nsu.ccfit.korovina.server.messagehandle.RoleChangeHandler;
import ru.nsu.ccfit.korovina.server.messagehandle.SteerHandler;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager {
    private DatagramSocket socket;
    private ConcurrentHashMap<DatagramPacket, Long> sentMessages;
    private Long seqNum;
    private ConcurrentHashMap<TypeCase, MessageHandler> messageHandlers;
    private Server server;
    private GameController gameController;
    private final Object monitor;

    public MessageManager(DatagramSocket socket) {
        this.socket = socket;
        this.sentMessages = new ConcurrentHashMap<>();
        this.seqNum = 0L;
        this.messageHandlers = new ConcurrentHashMap<>();
        this.monitor = new Object();
    }

    private class Resender extends TimerTask {
        @Override
        public void run() {
            if (GameController.isGameClosed()) {
                cancel();
            }
            sentMessages.forEach((packet, sendTime) -> {
                if (System.currentTimeMillis() - sendTime > 5000) {
                    try {
                        sendMessage(GameMessage.parseFrom(packet.getData()), packet.getAddress().getHostAddress(), packet.getPort());
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void removeConfirmedMessage(Long seq) {
//        System.out.println("sentMessages now: ");
        sentMessages.forEach((packet, sendTime) -> {
            try {
//                System.out.print(GameMessage.parseFrom(packet.getData()).getMsgSeq() + " ");
                if (GameMessage.parseFrom(packet.getData()).getMsgSeq() == seq) {
                    sentMessages.remove(packet);
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        });
//        System.out.println();
    }

    private class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                socket.setSoTimeout(3000);
                byte[] buffer = new byte[4096];
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                while (!GameController.isGameClosed()) {
                    try {
                        socket.receive(receivedPacket);
                    } catch (SocketTimeoutException e) {
//                        e.printStackTrace();
                        continue;
                    }
                    int packetSize = receivedPacket.getLength();
                    byte[] packet = Arrays.copyOfRange(receivedPacket.getData(), 0, packetSize);
                    GameMessage message = GameMessage.parseFrom(packet);
//                    System.out.println(GameController.role);
//                    System.out.println(message);
                    if (message.getTypeCase() == TypeCase.ACK) {
                        if (message.getMsgSeq() == 0 && !GameController.iAmMaster()) {
//                            System.out.println("my id is " + message.getReceiverId());
                            gameController.setPlayerId(message.getReceiverId());
                        }
                        if (GameController.iAmMaster()) {
                            //server.updateActivePlayers(message.getSenderId());
                        } else {
                            gameController.updateLastMsgFromMasterTime();
                        }
                        removeConfirmedMessage(message.getMsgSeq());
                    } else {
                      //  System.out.println(message);
                        synchronized (monitor) {
 //                           System.out.println(messageHandlers);
                            if (!messageHandlers.keySet().contains(message.getTypeCase())) {
                                System.err.println("No such handler: " + message.getTypeCase());
                                System.err.println("Message: " + message);
                                continue;
                            }
                            messageHandlers.get(message.getTypeCase()).handleMessage(message, receivedPacket.getAddress().getHostAddress(), receivedPacket.getPort());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runThreads() {
        Resender resender = new Resender();
        Timer rTimer = new Timer(true);
        rTimer.schedule(resender, 0, 5000);

        Thread receivingThread = new Thread(new Receiver());
        receivingThread.start();
    }

    public void updateSentMessages() {
        for (Map.Entry<DatagramPacket, Long> entry : sentMessages.entrySet()) {
            InetSocketAddress master = gameController.getMasterAddress();
            DatagramPacket packet = entry.getKey();
            Long sendTime = entry.getValue();
            DatagramPacket updatedPacket = new DatagramPacket(packet.getData(), packet.getData().length, master.getAddress(), master.getPort());
            sentMessages.remove(packet);
            sentMessages.put(updatedPacket, sendTime);
        }
    }

    public void updateClientMessageHandlers() {
        synchronized (monitor) {
            messageHandlers.clear();
            messageHandlers.put(GameMessage.TypeCase.ERROR, new ErrorHandler(gameController, this));
            messageHandlers.put(GameMessage.TypeCase.ROLE_CHANGE, new ClientRoleChangeHandler(gameController, this));
            messageHandlers.put(GameMessage.TypeCase.STATE, new StateHandler(gameController, this));
//            System.out.println("handlers updated " + messageHandlers);
        }
    }

    public void setClientMessageHandlers(GameController gameController) {
        this.gameController = gameController;
        updateClientMessageHandlers();
    }

    public void setServerMessageHandlers(Server server, GameController gameController) {
        this.server = server;
        this.gameController = gameController;
        synchronized (monitor) {
            messageHandlers.clear();
            messageHandlers.put(GameMessage.TypeCase.JOIN, new JoinHandler(gameController, server, this));
            messageHandlers.put(GameMessage.TypeCase.PING, new PingHandler(gameController, server, this));
            messageHandlers.put(GameMessage.TypeCase.ROLE_CHANGE, new RoleChangeHandler(gameController, server, this));
            messageHandlers.put(GameMessage.TypeCase.STEER, new SteerHandler(gameController, server, this));
            messageHandlers.put(GameMessage.TypeCase.STATE, new StateHandler(gameController, this));
        }
    }

    private boolean packetAlreadyExists(long seq) {
        for (DatagramPacket packet : sentMessages.keySet()) {
            try {
                if (GameMessage.parseFrom(packet.getData()).getMsgSeq() == seq) {
                    return true;
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    synchronized public void sendMessage(GameMessage message, String destAddress, int destPort) {
        try {
            DatagramPacket packet = new DatagramPacket(message.toByteArray(), message.toByteArray().length, InetAddress.getByName(destAddress), destPort);
            socket.send(packet);
            seqNum++;
            if (message.getTypeCase() != TypeCase.ACK && message.getTypeCase() != TypeCase.ANNOUNCEMENT) {
                if (!packetAlreadyExists(message.getMsgSeq())) {
                    sentMessages.put(packet, System.currentTimeMillis());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized public Long getSeqNum() { return seqNum; }

    synchronized public int getPort() { return this.socket.getLocalPort(); }
}
