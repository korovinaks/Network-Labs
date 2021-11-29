package ru.nsu.ccfit.korovina.menu;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class MenuView extends JFrame {
    private JPanel contentPane;
    private DefaultListModel<String> listModel;
    private JList<String> activeGamesList;
    private JTextField nameTextField;
    private JButton newGameButton;
    private JButton joinAsPlayerButton;
    private JButton joinAsViewerButton;
    private HashMap<State, Runnable> states;

    private void initControls() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());

        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        contentPane.add(panel3, new GridBagConstraints(0, 0, 1, 4, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Active games", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        activeGamesList = new JList();
        panel3.add(activeGamesList, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        contentPane.add(panel1, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,  new Insets(0, 0, 0, 0), 0, 0));
        panel1.setBorder(BorderFactory.createTitledBorder(null, "Name", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
        nameTextField = new JTextField();
        panel1.add(nameTextField, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        contentPane.add(panel2, new GridBagConstraints(1, 1, 1, 3, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        newGameButton = new JButton();
        newGameButton.setText("NEW GAME");
        panel2.add(newGameButton, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        joinAsPlayerButton = new JButton();
        joinAsPlayerButton.setText("Join as player");
        panel2.add(joinAsPlayerButton, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        joinAsViewerButton = new JButton();
        joinAsViewerButton.setText("Join as viewer");
        panel2.add(joinAsViewerButton, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }

    enum State {
        DEFAULT,
        NO_ACTIVE_GAMES,
        CONFIG_OPENED
    }

    MenuView() {
        initControls();

        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Menu");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.contentPane.setPreferredSize(new Dimension((int) (screenSize.getWidth() * 0.25), (int) (screenSize.getHeight() * 0.2)));
        this.setResizable(false);

        listModel = new DefaultListModel<>();
        activeGamesList.setModel(listModel);

        states = new HashMap<>();
        states.put(State.DEFAULT, () -> {
            newGameButton.setEnabled(true);
            joinAsPlayerButton.setEnabled(true);
            joinAsViewerButton.setEnabled(true);
            nameTextField.setEditable(true);
        });
        states.put(State.NO_ACTIVE_GAMES, () -> {
            joinAsPlayerButton.setEnabled(false);
            joinAsViewerButton.setEnabled(false);
        });
        states.put(State.CONFIG_OPENED, () -> {
            newGameButton.setEnabled(false);
            joinAsPlayerButton.setEnabled(false);
            joinAsViewerButton.setEnabled(false);
            nameTextField.setEditable(false);
        });

        this.pack();
    }

    void setState(State state) {
        states.get(state).run();
    }

    String getPlayerName() {
        return nameTextField.getText();
    }

    void addActiveGame(String game) {
        if (!listModel.contains(game)) {
            listModel.addElement(game);
            activeGamesList.setModel(listModel);
        }
    }

    public void removeFromList(String game) {
        listModel.remove(listModel.indexOf(game));
        activeGamesList.setModel(listModel);
    }

    void addNewGameActionListener(ActionListener l) {
        newGameButton.addActionListener(l);
    }

    void addJoinAsPlayerActionListener(ActionListener l) {
        joinAsPlayerButton.addActionListener(l);
    }

    void addJoinAsViewerActionListener(ActionListener l) {
        joinAsViewerButton.addActionListener(l);
    }

    boolean isNameNotSet() {
        return nameTextField.getText().equals("");
    }

    String getSelectedGame() {
        return activeGamesList.getSelectedValue();
    }
}
