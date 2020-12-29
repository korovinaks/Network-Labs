package ru.nsu.ccfit.korovina.config;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;

public class ConfigView extends JFrame {
    private JPanel contentPane;
    private JButton saveConfig;
    private JButton defaultConfig;

    private JSlider width;
    private JSlider height;
    private JSlider foodStatic;
    private JFormattedTextField foodPerPlayer;
    private JSlider stateDelayMs;
    private JFormattedTextField deadFoodProb;
    private JSlider pingDelayMs;
    private JSlider nodeTimeoutMs;
    private JLabel widthValue, heightValue, foodStaticValue,
            stateDelayValue, pingDelayValue, nodeTimeoutValue;

    ConfigView() {
        initControls();

        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Config");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setPreferredSize(new Dimension((int) (screenSize.getWidth() * 0.3), (int) (screenSize.getHeight() * 0.6)));
        this.setResizable(false);

        width.addChangeListener((ChangeEvent e) -> {
            widthValue.setText(Integer.toString(width.getValue()));
        });

        height.addChangeListener((ChangeEvent e) -> {
            heightValue.setText(Integer.toString(height.getValue()));
        });

        foodStatic.addChangeListener((ChangeEvent e) -> {
            foodStaticValue.setText(Integer.toString(foodStatic.getValue()));
        });

        stateDelayMs.addChangeListener((ChangeEvent e) -> {
            stateDelayValue.setText(Integer.toString(stateDelayMs.getValue()));
        });

        pingDelayMs.addChangeListener((ChangeEvent e) -> {
            pingDelayValue.setText(Integer.toString(pingDelayMs.getValue()));
        });

        nodeTimeoutMs.addChangeListener((ChangeEvent e) -> {
            nodeTimeoutValue.setText(Integer.toString(nodeTimeoutMs.getValue()));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConfigView dialog = new ConfigView();
            dialog.pack();
            dialog.setVisible(true);
        });
    }

    public void addSaveConfigActionListener(ActionListener l) {
        saveConfig.addActionListener(l);
    }

    public void addDefaultConfigActionListener(ActionListener l) {
        defaultConfig.addActionListener(l);
    }

    public int getWidthValue() {
        return this.width.getValue();
    }

    public int getHeightValue() {
        return this.height.getValue();
    }

    public int getFoodStatic() {
        return this.foodStatic.getValue();
    }

    public float getFoodPerPlayer() {
        return Float.valueOf(this.foodPerPlayer.getText());
    }

    public int getStateDelayMs() {
        return this.stateDelayMs.getValue();
    }

    public float getDeadFoodProb() {
        return Float.valueOf(this.deadFoodProb.getText());
    }

    public int getPingDelayMs() {
        return this.pingDelayMs.getValue();
    }

    public int getNodeTimeoutMs() {
        return this.nodeTimeoutMs.getValue();
    }

    private void initControls() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());

        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        contentPane.add(panel1, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Width", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        width = new JSlider();
        width.setMinimum(10);
        width.setValue(40);
        panel1.add(width, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        contentPane.add(panel2,  new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Height", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        height = new JSlider();
        height.setMinimum(10);
        height.setValue(30);
        panel2.add(height, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        contentPane.add(panel3,  new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Total food", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        foodStatic = new JSlider();
        foodStatic.setValue(1);
        panel3.add(foodStatic, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        contentPane.add(panel4,  new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Food per player", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        foodPerPlayer = new JFormattedTextField();
        foodPerPlayer.setText("1.0");
        panel4.add(foodPerPlayer, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        contentPane.add(panel5,  new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "State delay", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        stateDelayMs = new JSlider();
        stateDelayMs.setMaximum(10000);
        stateDelayMs.setMinimum(1);
        stateDelayMs.setValue(1000);
        panel5.add(stateDelayMs, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridBagLayout());
        contentPane.add(panel6,  new GridBagConstraints(0, 6, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Ping delay", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        pingDelayMs = new JSlider();
        pingDelayMs.setMaximum(10000);
        pingDelayMs.setMinimum(1);
        pingDelayMs.setValue(100);
        panel6.add(pingDelayMs, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridBagLayout());
        contentPane.add(panel7,  new GridBagConstraints(0, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Probability of dead snakes becoming food", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        deadFoodProb = new JFormattedTextField();
        deadFoodProb.setText("0.1");
        panel7.add(deadFoodProb, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridBagLayout());
        contentPane.add(panel8,  new GridBagConstraints(0, 7, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Node timeout", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        nodeTimeoutMs = new JSlider();
        nodeTimeoutMs.setMaximum(10000);
        nodeTimeoutMs.setMinimum(1);
        nodeTimeoutMs.setValue(800);
        panel8.add(nodeTimeoutMs, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 0, 0, 0), 0, 0));

        saveConfig = new JButton();
        saveConfig.setText("Save");
        contentPane.add(saveConfig,  new GridBagConstraints(0, 8, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));

        defaultConfig = new JButton();
        defaultConfig.setText("Default");
        contentPane.add(defaultConfig,  new GridBagConstraints(0, 9, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));

        widthValue = new JLabel();
        widthValue.setText("40");
        contentPane.add(widthValue, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        heightValue = new JLabel();
        heightValue.setText("30");
        contentPane.add(heightValue, new GridBagConstraints(1, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        foodStaticValue = new JLabel();
        foodStaticValue.setText("1");
        contentPane.add(foodStaticValue, new GridBagConstraints(1, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        stateDelayValue = new JLabel();
        stateDelayValue.setText("1000");
        contentPane.add(stateDelayValue, new GridBagConstraints(1, 4, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        pingDelayValue = new JLabel();
        pingDelayValue.setText("100");
        contentPane.add(pingDelayValue, new GridBagConstraints(1, 6, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        nodeTimeoutValue = new JLabel();
        nodeTimeoutValue.setText("800");
        contentPane.add(nodeTimeoutValue, new GridBagConstraints(1, 7, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
    }

}

