package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private int port;

    public Server (int port) {
        this.port = port;
    }

    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        try {
            ServerSocket server = new ServerSocket(port);

            while(true) {
                Socket client = server.accept();
                executorService.execute(new ClientWorker(client));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
