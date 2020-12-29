package ru.nsu.ccfit.korovina.game.gameview;

import me.ippolitov.fit.snakes.SnakesProto.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class GameView extends JFrame {
    private JPanel contentPane;
    private GameField gameField;
    private JPanel controlsPane;
    private JLabel listTitle;
    private JPanel listPane;
    private JList<String> playersList;
    private DefaultListModel<String> listModel;
    private JButton becomeAViewer;
    public GameStateDesigner gameStateDesigner;


    public GameView(int cellsHorizontal, int cellsVertical) {
        initControls();

        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Snakes");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int contentPaneWidth = (int) (screenSize.getWidth() * 0.8);
        int contentPaneHeight = (int) (screenSize.getHeight() * 0.8);
        this.setResizable(false);
        contentPane.setLayout(new BorderLayout());

        this.gameField = new GameField(cellsHorizontal, cellsVertical, contentPaneWidth, contentPaneHeight);
        contentPane.add(gameField, BorderLayout.WEST);
        gameField.updateGameField(new ArrayList<>(), new ArrayList<>());
        gameField.repaint();

        int controlsPaneWidth, controlsPaneHeight;
        controlsPaneWidth = (int) (screenSize.getWidth() * 0.2);
        controlsPaneHeight = gameField.getGameFieldHeight();

        controlsPane = new JPanel();
        controlsPane.setPreferredSize(new Dimension(controlsPaneWidth, controlsPaneHeight));
        controlsPane.setLayout(new BorderLayout());

        listTitle = new JLabel();
        listTitle.setText("Players");
        listTitle.setForeground(Color.LIGHT_GRAY);

        listModel = new DefaultListModel<>();
        playersList = new JList<>();
        playersList.setModel(listModel);
        playersList.setBackground(Color.LIGHT_GRAY);
        playersList.setEnabled(false);
        listPane = new JPanel();
        listPane.setBorder(new LineBorder(Color.DARK_GRAY, 1, false));
        listPane.setLayout(new BorderLayout());
        listPane.add(playersList, BorderLayout.CENTER);

        becomeAViewer = new JButton();
        becomeAViewer.setText("BECOME A VIEWER");
        becomeAViewer.setBackground(Color.LIGHT_GRAY);

        controlsPane.add(listPane, BorderLayout.CENTER);
        controlsPane.add(listTitle, BorderLayout.NORTH);
        controlsPane.add(becomeAViewer, BorderLayout.SOUTH);

        contentPane.add(controlsPane, BorderLayout.EAST);
        this.pack();
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void clearPlayersList() {
        listModel = new DefaultListModel<>();
        playersList.setModel(listModel);
    }

    public void addPlayer(GamePlayer gamePlayer) {
        String player = gamePlayer.getName() + " " + gamePlayer.getScore();
        listModel.addElement(player);
        playersList.setModel(listModel);
    }

    GameField getGameField() {
        return gameField;
    }

    public void addBecomeAViewerActionListener(ActionListener l) {
        becomeAViewer.addActionListener(l);
    }


    private void initControls() {
        contentPane = new JPanel();
    }
}
