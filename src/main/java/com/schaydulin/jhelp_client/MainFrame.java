package com.schaydulin.jhelp_client;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import command.Command;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class MainFrame extends JFrame {

    private JPanel panel;
    private JTabbedPane tabbedPane;
    private JPanel menuPanel;
    private JMenuBar menuBar;
    private JPanel tabPanel;
    private JPanel mainTab;
    private JPanel settingsTab;
    private JPanel helpTab;
    private JPanel contentPanel;
    private JPanel buttonsPanel;
    private JButton findButton;
    private JButton addButton;
    private JButton deleteButton;
    private JButton nextButton;
    private JButton previousButton;
    private JTextField termTextField;
    private JPanel termPanel;
    private JPanel textPanel;
    private JTextArea definitionsTextArea;
    private JButton exitButton;
    private JScrollPane scrollPane;
    private JScrollBar scrollBar1;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu settingsMenu;
    private JMenu helpMenu;

    private static final int SERVER_PORT = 16105;

    private static final String NOT_FOUND = "Definition not found";

    private Map<String, List<String>> cashedDefinitionsMap = new HashMap<>();

    private String currentTerm;

    private byte currentDefinitionIndex = -1;

    MainFrame() {

        super("JHelp Client");

        $$$setupUI$$$();
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(800, 600);
        setResizable(true);
        setVisible(true);

        enableButtons(false, nextButton, previousButton);

        findButton.addActionListener(e -> {

            currentTerm = termTextField.getText().toLowerCase();

            definitionsTextArea.setText("");

            currentDefinitionIndex = -1;

            enableButtons(false, nextButton, previousButton);

            if (currentTerm.isBlank())
                return;

            if (!cashedDefinitionsMap.containsKey(currentTerm)) {

                try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {

                    try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream());
                         ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                        ous.writeObject(Command.FIND);

                        ous.writeObject(currentTerm);

                        ous.flush();

                        String def = (String) ois.readObject();

                        if (def == null)

                            definitionsTextArea.setText(NOT_FOUND);

                        else {

                            cashedDefinitionsMap.put(currentTerm, new LinkedList<>());

                            cashedDefinitionsMap.get(currentTerm).add(def);

                            while (true) {

                                def = (String) ois.readObject();

                                cashedDefinitionsMap.get(currentTerm).add(def);

                            }

                        }

                    }

                } catch (EOFException eof) {

                    showFirstDefinition();

                } catch (IOException | ClassNotFoundException ex) {

                    ex.printStackTrace();

                }

            } else showFirstDefinition();

        });

        addButton.addActionListener(e -> {

            String term = termTextField.getText().toLowerCase();

            String definition = definitionsTextArea.getText();

            if (term.isBlank() && definition.isBlank()) {
                showWarning("Nothing to add", "Empty arguments");
                return;
            } else if (term.isBlank()) {
                showWarning("Term can not be empty", "Empty term");
                return;
            } else if (definition.isBlank()) {
                showWarning("Definition can not be empty", "Empty definition");
                return;
            }

            try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {
                try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream())) {

                    ous.writeObject(Command.ADD);

                    ous.writeObject(term);

                    ous.writeObject(definition);

                    ous.flush();

                    showInfo("Successfully added new definition. Restart server to commit changes", "Done");

                }

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        });

        deleteButton.addActionListener(e ->

        {

            String term = termTextField.getText().toLowerCase();

            String definition = definitionsTextArea.getText();

            if (term.isBlank() && definition.isBlank())

                showWarning("Nothing to delete", "Empty arguments");

            else if (term.isBlank())

                showWarning("Term can not be empty", "Empty term");

            else {

                try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {
                    try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream())) {

                        ous.writeObject(Command.DELETE_TERM);

                        ous.writeObject(term);

                        ous.flush();

                        showInfo("Term successfully deleted. Restart server to commit changes", "Done");

                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }

        });

        nextButton.addActionListener(e ->

        {

            definitionsTextArea.setText("");
            definitionsTextArea.append(cashedDefinitionsMap.get(currentTerm).get(++currentDefinitionIndex));
            enableButtons(true, previousButton);
            if (currentDefinitionIndex == cashedDefinitionsMap.get(currentTerm).size() - 1)
                enableButtons(false, nextButton);

        });

        previousButton.addActionListener(e ->

        {

            definitionsTextArea.setText("");
            definitionsTextArea.append(cashedDefinitionsMap.get(currentTerm).get(--currentDefinitionIndex));
            enableButtons(true, nextButton);
            if (currentDefinitionIndex == 0)
                enableButtons(false, previousButton);

        });

        exitButton.addActionListener(e -> System.exit(0));

    }

    private void enableButtons(boolean enable, JButton... buttons) {

        for (JButton button : buttons)
            button.setEnabled(enable);

    }

    private void showFirstDefinition() {

        definitionsTextArea.append(cashedDefinitionsMap.get(currentTerm).get(++currentDefinitionIndex));

        if (currentDefinitionIndex < cashedDefinitionsMap.get(currentTerm).size() - 1)
            enableButtons(true, nextButton);

    }

    private void showWarning(String message, String title) {

        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);

    }

    private void showInfo(String message, String title) {

        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here

        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        settingsMenu = new JMenu("Settings");
        helpMenu = new JMenu("Help");

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

    }

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(MainFrame.class, args);

        SwingUtilities.invokeLater(() -> context.getBean(MainFrame.class));

    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        menuPanel = new JPanel();
        menuPanel.setLayout(new BorderLayout(0, 0));
        panel.add(menuPanel, BorderLayout.NORTH);
        menuPanel.add(menuBar, BorderLayout.NORTH);
        tabPanel = new JPanel();
        tabPanel.setLayout(new BorderLayout(0, 0));
        panel.add(tabPanel, BorderLayout.CENTER);
        tabbedPane = new JTabbedPane();
        tabPanel.add(tabbedPane, BorderLayout.CENTER);
        mainTab = new JPanel();
        mainTab.setLayout(new BorderLayout(15, 0));
        tabbedPane.addTab("Main", mainTab);
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(0, 0));
        mainTab.add(contentPanel, BorderLayout.CENTER);
        termPanel = new JPanel();
        termPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 5, 10, 10), 0, 0));
        contentPanel.add(termPanel, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Term:");
        termPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        termTextField = new JTextField();
        termPanel.add(termTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, 30), null, 0, false));
        textPanel = new JPanel();
        textPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(textPanel, BorderLayout.CENTER);
        final JLabel label2 = new JLabel();
        label2.setText("Definitions:");
        textPanel.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(22);
        textPanel.add(scrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        definitionsTextArea = new JTextArea();
        definitionsTextArea.setLineWrap(true);
        definitionsTextArea.setText("");
        scrollPane.setViewportView(definitionsTextArea);
        scrollBar1 = new JScrollBar();
        textPanel.add(scrollBar1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayoutManager(9, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainTab.add(buttonsPanel, BorderLayout.EAST);
        findButton = new JButton();
        findButton.setText("Find");
        buttonsPanel.add(findButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(58, 30), null, 0, false));
        addButton = new JButton();
        addButton.setText("Add");
        buttonsPanel.add(addButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(58, 30), null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("Delete");
        buttonsPanel.add(deleteButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(58, 30), null, 0, false));
        nextButton = new JButton();
        nextButton.setEnabled(true);
        nextButton.setText("Next");
        buttonsPanel.add(nextButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(58, 30), null, 0, false));
        previousButton = new JButton();
        previousButton.setEnabled(true);
        previousButton.setHideActionText(false);
        previousButton.setText("Previous");
        buttonsPanel.add(previousButton, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(58, 30), null, 0, false));
        final Spacer spacer1 = new Spacer();
        buttonsPanel.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(58, 14), null, 0, false));
        final Spacer spacer2 = new Spacer();
        buttonsPanel.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(58, 14), null, 0, false));
        final Spacer spacer3 = new Spacer();
        buttonsPanel.add(spacer3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        buttonsPanel.add(spacer4, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(58, 14), null, 0, false));
        exitButton = new JButton();
        exitButton.setText("Exit");
        buttonsPanel.add(exitButton, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        settingsTab = new JPanel();
        settingsTab.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab("Settings", settingsTab);
        helpTab = new JPanel();
        helpTab.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab("Help", helpTab);
        scrollPane.setVerticalScrollBar(scrollBar1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }

}
