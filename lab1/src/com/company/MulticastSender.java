package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class MulticastSender {

    private final static int CHECK_TIMEOUT = 1000;
    private final static String message = "Hello";
    private long lastTime;
    protected int port;
    protected InetAddress group;

    public MulticastSender ( InetAddress group, int port) {
        this.port = port;
        this.group = group;
        lastTime = System.currentTimeMillis();
    }

    public void send() {
        if (System.currentTimeMillis() - lastTime <= CHECK_TIMEOUT) {
            return;
        }

        lastTime = System.currentTimeMillis();

        try {
            MulticastSocket sender = new MulticastSocket();

            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, port);
            sender.send(packet);

            sender.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
