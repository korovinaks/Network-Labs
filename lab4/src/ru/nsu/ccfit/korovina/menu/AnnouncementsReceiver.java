package ru.nsu.ccfit.korovina.menu;

import com.google.protobuf.InvalidProtocolBufferException;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import ru.nsu.ccfit.korovina.utils.Pair;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementsReceiver implements Runnable {
    private ConcurrentHashMap<String, Pair<GamePlayer, AnnouncementMsg>> activeGames;
    private MulticastSocket multicastSocket;
    private ConcurrentHashMap<String, Long> receiveTime;

    AnnouncementsReceiver(ConcurrentHashMap<String, Long> receiveTime, ConcurrentHashMap<String, Pair<GamePlayer, AnnouncementMsg>> activeGames,
                          MulticastSocket multicastSocket) {
        this.activeGames = activeGames;
        this.multicastSocket = multicastSocket;
        this.receiveTime = receiveTime;
    }

    private void processMessage(DatagramPacket receivedPacket) {
        try {
            int packetSize = receivedPacket.getLength();
            byte[] packet = Arrays.copyOfRange(receivedPacket.getData(), 0, packetSize);
            GameMessage message = GameMessage.parseFrom(packet);
            if (message.getTypeCase() == TypeCase.ANNOUNCEMENT) {
                AnnouncementMsg anncMsg = message.getAnnouncement();
                List<GamePlayer> players = new ArrayList<>(anncMsg.getPlayers().getPlayersList());
                players.forEach((GamePlayer player) -> {
                    if (player.getRole() == NodeRole.MASTER) {
                        int index = players.indexOf(player);
                        player = GamePlayer.newBuilder()
                                .setName(player.getName())
                                .setId(player.getId())
                                .setIpAddress(receivedPacket.getAddress().getHostName())
                                .setPort(player.getPort())
                                .setRole(player.getRole())
                                .setScore(player.getScore())
                                .build();
                        players.set(index, player);
                        String gameDescription = player.getName() + " [" + receivedPacket.getAddress().getHostAddress() + "] " + anncMsg.getConfig().getWidth() +
                                "x" + anncMsg.getConfig().getHeight();
                        activeGames.put(gameDescription, new Pair<>(player, anncMsg));
                        receiveTime.put(gameDescription, System.currentTimeMillis());
                    }
                });
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            multicastSocket.joinGroup(InetAddress.getByName("239.192.0.4"));
            multicastSocket.setSoTimeout(3000);
            byte[] buffer = new byte[4096];
            DatagramPacket message = new DatagramPacket(buffer, buffer.length);
            while (!MenuController.isClosed().get()) {
                try {
                    multicastSocket.receive(message);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                processMessage(message);
            }
            multicastSocket.leaveGroup(InetAddress.getByName("239.192.0.4"));
            multicastSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
