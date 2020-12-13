package com.company;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Map;
import java.util.UUID;

public class SendMessagesThread extends Thread {

    private NodeServer node_server;

    public SendMessagesThread(NodeServer node_server) {
        this.node_server = node_server;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Map.Entry<UUID, MessageObject> message_to_deliver = node_server.get_message_to_deliver();

                if (message_to_deliver == null) {
                    sleep(300);
                    continue;
                }

                DatagramSocket sender = new DatagramSocket();

                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message_to_deliver.getKey());
                oos.writeObject(message_to_deliver.getValue());

                InetAddress ip_target = message_to_deliver.getValue().get_ip_target();
                int port_target = message_to_deliver.getValue().get_port_target();
                DatagramPacket packet = new DatagramPacket(baos.toByteArray(), baos.size(), ip_target, port_target);

                sender.send(packet);

                sender.close();

                node_server.add_to_confirmation_map(message_to_deliver.getKey(), message_to_deliver.getValue());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
