package com.company;

import java.net.InetAddress;

public class Main {
    private final static int port = 33333;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify IP multicast group address");
            return;
        }
        try {
            InetAddress group = InetAddress.getByName(args[0]);

            MulticastSender sender = new MulticastSender(group, port);
            sender.start();

            MulticastReceiver receiver = new MulticastReceiver(group, port);
            receiver.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
