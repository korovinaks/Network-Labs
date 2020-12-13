package com.company;

import java.util.Scanner;

public class ReadMessagesThread extends Thread {

    private NodeServer node_server;

    public ReadMessagesThread(NodeServer node_server) {
        this.node_server = node_server;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String message;

        while (true) {
            message = scanner.nextLine();
            node_server.new_message(message);
        }
    }
}
