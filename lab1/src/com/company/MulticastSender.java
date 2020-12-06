package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class MulticastSender extends Thread {

    private final static int sleep_time = 1000;
    private final static String message = "Hello";
    protected int port;
    protected InetAddress group;

    public MulticastSender ( InetAddress group, int port) {
        this.port = port;
        this.group = group;
    }

    public void run() {
        while(true)  {
            try {
                MulticastSocket sender = new MulticastSocket();

                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, port);
                sender.send(packet);

                sender.close();

                sleep(sleep_time);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
