package ru.nsu.ccfit.korovina.config;

import me.ippolitov.fit.snakes.SnakesProto.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConfigController {
    private GameConfig config;
    private ConfigView configView;
    private Runnable newGameCallback;

    public ConfigController(Runnable menuEnableCallback) {
        configView = new ConfigView();
        configView.pack();
        configView.setVisible(true);

        configView.addSaveConfigActionListener(this::saveNew);
        configView.addDefaultConfigActionListener(this::saveDefault);
        configView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                menuEnableCallback.run();
                configView.dispose();
            }
        });
    }

    public void setNewGameCallback(Runnable newGame) {
        this.newGameCallback = newGame;
    }

    private void saveNew(ActionEvent e) {
        this.config = GameConfig.newBuilder()
                .setWidth(configView.getWidthValue())
                .setHeight(configView.getHeightValue())
                .setFoodStatic(configView.getFoodStatic())
                .setFoodPerPlayer(configView.getFoodPerPlayer())
                .setStateDelayMs(configView.getStateDelayMs())
                .setDeadFoodProb(configView.getDeadFoodProb())
                .setPingDelayMs(configView.getPingDelayMs())
                .setNodeTimeoutMs(configView.getNodeTimeoutMs())
                .build();

        if (config.getPingDelayMs() > config.getNodeTimeoutMs()) {
            JOptionPane.showMessageDialog(configView, "Ping delay should be smaller than node timeout.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (config.getFoodPerPlayer() < 0 || config.getFoodPerPlayer() > 100) {
            JOptionPane.showMessageDialog(configView, "Food per player should be between 0 and 100.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (config.getDeadFoodProb() < 0 || config.getDeadFoodProb() > 1) {
            JOptionPane.showMessageDialog(configView, "Probability should be between 0 and 100.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        newGameCallback.run();
        configView.dispose();
    }

    private void saveDefault(ActionEvent e) {
        this.config = GameConfig.newBuilder()
                .setWidth(40)
                .setHeight(30)
                .setFoodStatic(1)
                .setFoodPerPlayer(1)
                .setStateDelayMs(1000)
                .setDeadFoodProb(0.1f)
                .setPingDelayMs(100)
                .setNodeTimeoutMs(800)
                .build();

        newGameCallback.run();
        configView.dispose();
    }

    public GameConfig getConfig() { return config; }
}
