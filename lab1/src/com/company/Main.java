package com.company;

import java.net.InetAddress;

import static java.lang.Thread.sleep;

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

            MulticastReceiver receiver = new MulticastReceiver(group, port);

            while (true) {
                sender.send();

                receiver.read();

                sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
