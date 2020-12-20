package com.company;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.UUID;

public class ReceiveMessagesThread extends Thread {

    private NodeServer node_server;

    public ReceiveMessagesThread(NodeServer node_server) {
        this.node_server = node_server;
    }

    @Override
    public void run() {
        byte[] buf = new byte[4096];
        Random random = new Random();
        try {
            DatagramSocket receiver = new DatagramSocket(node_server.get_port());

            while(true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receiver.receive(packet);
                InetAddress ip_sender = packet.getAddress();

                if (random.nextInt(100) < node_server.get_loss_percent()) {
                    System.out.println("Packet lost");
                    continue;
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                ObjectInputStream ois = new ObjectInputStream(bais);
                UUID id = (UUID) ois.readObject();
                MessageObject message_delivered = (MessageObject) ois.readObject();
                int port_sender = message_delivered.get_port_sender();

                ois.close();
                bais.close();

                node_server.new_message_received(id, message_delivered, ip_sender, port_sender);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
