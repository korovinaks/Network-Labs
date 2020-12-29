package ru.nsu.ccfit.korovina;

import ru.nsu.ccfit.korovina.menu.MenuController;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;

public class Application {

    public static void main(String[] args) {
        Random rand = new Random(System.currentTimeMillis());
        int port = rand.nextInt(1000) + 9000;
        try (DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
             MulticastSocket multicastSocket = new MulticastSocket(9192)) {

            MessageManager messageManager = new MessageManager(socket);
            MenuController menuController = new MenuController(messageManager, multicastSocket);
            menuController.runActiveGamesUpdater();
            synchronized (MenuController.isClosed()) {
                while (!MenuController.isClosed().get()) {
                    MenuController.isClosed().wait();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
