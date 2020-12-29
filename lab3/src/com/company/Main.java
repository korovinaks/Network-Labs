package com.company;

import java.net.InetAddress;

public class Main {

    public static void main(String[] args) {

        if (args.length != 3 && args.length != 5) {
            System.out.println("Please specify correct number of parameters");
            return;
        }

        String node_name = args[0];
        int port = Integer.parseInt(args[1]);
        int loss_percent = Integer.parseInt(args[2]);

        try {

            NodeServer node_server = new NodeServer(node_name, port, loss_percent);

            if (args.length == 5) {
                InetAddress parent_ip = InetAddress.getByName(args[3]);
                int parent_port = Integer.parseInt(args[4]);

                node_server.set_parent(parent_ip, parent_port);
            }

            node_server.run();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
