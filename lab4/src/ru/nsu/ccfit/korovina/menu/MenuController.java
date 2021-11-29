package ru.nsu.ccfit.korovina.menu;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.AnnouncementMsg;
import ru.nsu.ccfit.korovina.config.ConfigController;
import ru.nsu.ccfit.korovina.game.GameController;
import ru.nsu.ccfit.korovina.game.gameview.GameStateDesigner;
import ru.nsu.ccfit.korovina.messagemanagement.MessageManager;
import ru.nsu.ccfit.korovina.server.Server;
import ru.nsu.ccfit.korovina.utils.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MenuController {
    private MenuView menuView;
    private ConcurrentHashMap<String, Pair<GamePlayer, AnnouncementMsg>> activeGames;
    private ConcurrentHashMap<String, Long> receiveTime;
    private AnnouncementsReceiver updater;
    private static AtomicBoolean isClosed;
    private MessageManager messageManager;
    private Runnable activateMenuCallback;

    public MenuController(MessageManager messageManager, MulticastSocket multicastSocket) {
        this.activeGames = new ConcurrentHashMap<>();
        this.receiveTime = new ConcurrentHashMap<>();
        this.updater = new AnnouncementsReceiver(receiveTime, activeGames, multicastSocket);
        this.messageManager = messageManager;
        this.activateMenuCallback = () -> {
            menuView.setVisible(true);
            menuView.setState(MenuView.State.DEFAULT);
        };
        MenuController.isClosed = new AtomicBoolean(false);

        menuView = new MenuView();
        menuView.pack();
        menuView.setVisible(true);

        menuView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                synchronized (isClosed()) {
                    isClosed.set(true);
                    isClosed().notifyAll();
                }
            }
        });

        menuView.addNewGameActionListener(this::startNewGame);
        menuView.addJoinAsPlayerActionListener(this::joinAsPlayer);
        menuView.addJoinAsViewerActionListener(this::joinAsViewer);
    }

    private class ListUpdater extends TimerTask {
        @Override
        public void run() {
            if (isClosed.get()) {
                cancel();
            }
            activeGames.forEach((string, game) -> {
                Long lastReceiveTime = receiveTime.get(string);
                if (System.currentTimeMillis() - lastReceiveTime > 1000) {
                    activeGames.remove(string);
                    menuView.removeFromList(string);
                    System.out.println("removed");
                }
            });
            activeGames.forEach((string, game) -> {
                menuView.addActiveGame(string);
            });
            if (activeGames.isEmpty()) {
                menuView.setState(MenuView.State.NO_ACTIVE_GAMES);
            } else {
                menuView.setState(MenuView.State.DEFAULT);
            }
        }
    }

    public void runActiveGamesUpdater() {
        Thread anncReceivingThread = new Thread(updater);
        anncReceivingThread.start();
        ListUpdater listUpdaterTask = new ListUpdater();
        Timer timer = new Timer(true);
        timer.schedule(listUpdaterTask, 0, 1000);
    }

    private void startNewGame(ActionEvent e) {
        if (menuView.isNameNotSet()) {
            JOptionPane.showMessageDialog(menuView, "Please enter your name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        menuView.setState(MenuView.State.CONFIG_OPENED);

        ConfigController configController = new ConfigController(() -> {
                menuView.setState(MenuView.State.DEFAULT);
        });

        configController.setNewGameCallback(() -> {
            Server server = new Server(messageManager, configController.getConfig(), 0);
            server.createPlayer(menuView.getPlayerName(), NodeRole.MASTER, "localhost", messageManager.getPort());

            GameController gameController = new GameController(NodeRole.MASTER, 0, new InetSocketAddress("localhost",
                    messageManager.getPort()), configController.getConfig(), messageManager, activateMenuCallback);
            gameController.runServices();
            gameController.getGameView().gameStateDesigner = new GameStateDesigner(gameController);
            server.setMasterId(0);

            messageManager.setServerMessageHandlers(server, gameController);
            messageManager.runThreads();
            server.run();

            menuView.setVisible(false);
        });
    }

    private void tryToJoin(NodeRole role) {
        if (menuView.isNameNotSet()) {
            JOptionPane.showMessageDialog(menuView, "Please enter your name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String select = menuView.getSelectedGame();
        if (select == null) {
            JOptionPane.showMessageDialog(menuView, "Please choose a game", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Pair<GamePlayer, AnnouncementMsg> selectedGame = activeGames.get(select);
        GamePlayer master = selectedGame.getKey();
        AnnouncementMsg announcement = activeGames.get(menuView.getSelectedGame()).getValue();
        GameConfig config = announcement.getConfig();

        if (!announcement.getCanJoin()) {
            JOptionPane.showMessageDialog(menuView, "Server is full. Please select another game", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
        GameController gameController = new GameController(role, master.getId(), new InetSocketAddress(InetAddress.getByName(master.getIpAddress()),
                                                            master.getPort()), config, messageManager, activateMenuCallback);
            gameController.getGameView().gameStateDesigner = new GameStateDesigner(gameController);
            messageManager.setClientMessageHandlers(gameController);
            messageManager.runThreads();
            GameMessage join = GameMessage.newBuilder()
                            .setJoin(GameMessage.JoinMsg.newBuilder()
                                    .setOnlyView(!role.equals(NodeRole.NORMAL))
                                    .setName(menuView.getPlayerName())
                                    .build())
                            .setMsgSeq(messageManager.getSeqNum())
                            .build();
            messageManager.sendMessage(join, master.getIpAddress(), master.getPort());
            gameController.runServices();
//            System.out.println("my master's address is " + InetAddress.getByName(master.getIpAddress()) + ":" + master.getPort());
            menuView.setVisible(false);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    private void joinAsPlayer(ActionEvent e) { tryToJoin(NodeRole.NORMAL); }

    private void joinAsViewer(ActionEvent e) { tryToJoin(NodeRole.VIEWER); }

    public static AtomicBoolean isClosed() {return MenuController.isClosed; }
}
