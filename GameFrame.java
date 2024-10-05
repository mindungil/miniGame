package HandGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameFrame extends JFrame {
    private JTextField playerNameField;
    private JLabel playerLabel;
    private JLabel player2Label;
    private JPanel playerNamePanel;
    private JLabel timeLabel;
    private String playerName;
    private String player2Name;
    private int playerGauge = 490;
    private int playerPower = 3;
    private int player2Gauge = 490;
    private int player2Power = 3;
    private boolean gameRunning = false;
    private List<String> matchHistory = new ArrayList<>();
    private DefaultListModel<String> matchHistoryModel;
    private Font deFont = new Font("deFont", Font.BOLD, 25);
    private JPanel mainPanel;
    private JPanel gaugePanel;
    private JPanel playerPanel;
    private JPanel player2Panel;
    private boolean shiftKeyPressed = false;
    private boolean isPlayer1 = false;
    private Random random = new Random();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean player1PowerBoostActive;
    private char player1RandomKey;
    private char player2RandomKey;
    private JLabel player1RandomKeyLabel;
    private JLabel player2RandomKeyLabel;
    private boolean player2PowerBoostActive;

    public GameFrame() throws IOException {
        socket = new Socket("localhost", 9004);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        setTitle("팔씨름 게임");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new CardLayout());
        gaugePanel = new JPanel(new BorderLayout());
        gaugePanel.setOpaque(true);
        gaugePanel.setBackground(Color.BLUE);
        playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBackground(Color.RED);
        playerPanel.setPreferredSize(new Dimension(400, 40));
        player2Panel = new JPanel(new BorderLayout());
        player2Panel.setBackground(Color.BLUE);
        player2Panel.setPreferredSize(new Dimension(400, 40));
        gaugePanel.add(playerPanel, BorderLayout.WEST);
        gaugePanel.add(player2Panel, BorderLayout.EAST);

        BackgroundPanel namePanel = new BackgroundPanel("startBackground.png");
        namePanel.setLayout(new GridLayout(5, 1));
        JPanel playerPanelInput = new JPanel(new FlowLayout());
        playerPanelInput.setOpaque(false);
        JLabel nameLabel = new JLabel("Player 이름:");
        nameLabel.setFont(deFont);
        playerPanelInput.add(nameLabel);
        playerNameField = new JTextField(20);
        playerPanelInput.add(playerNameField);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        JButton submitButton = new JButton("이름 제출");
        submitButton.addActionListener(e -> submitName());
        buttonPanel.add(submitButton);
        namePanel.add(new JLabel(""));
        namePanel.add(playerPanelInput);
        namePanel.add(buttonPanel);
        namePanel.add(new JLabel(""));
        mainPanel.add(namePanel, "namePanel");

        BackgroundPanel gamePanel = new BackgroundPanel("gameBackground.png");
        gamePanel.setLayout(new GridLayout(6, 1));
        playerLabel = new JLabel("", JLabel.CENTER);
        playerLabel.setFont(deFont);
        playerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        player2Label = new JLabel("", JLabel.CENTER);
        player2Label.setFont(deFont);
        player2Label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        playerNamePanel = new JPanel(new GridBagLayout());
        playerNamePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        playerNamePanel.add(playerLabel, gbc);
        gbc.gridx = 1;
        playerNamePanel.add(player2Label, gbc);
        timeLabel = new JLabel("남은 시간: ", JLabel.CENTER);
        timeLabel.setFont(deFont);
        timeLabel.setForeground(Color.RED);
        gamePanel.add(playerNamePanel);
        gamePanel.add(timeLabel);
        gamePanel.add(gaugePanel);
        mainPanel.add(gamePanel, "gamePanel");

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("상대 전적"));
        historyPanel.setPreferredSize(new Dimension(200, getHeight()));
        matchHistoryModel = new DefaultListModel<>();
        JList<String> matchHistoryList = new JList<>(matchHistoryModel);
        JScrollPane scrollPane = new JScrollPane(matchHistoryList);
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        add(historyPanel, BorderLayout.EAST);
        setMinimumSize(new Dimension(1000, 800));
        setPreferredSize(new Dimension(1000, 800));
        setResizable(false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameRunning && e.getKeyCode() == KeyEvent.VK_SHIFT && !shiftKeyPressed) {
                    shiftKeyPressed = true;
                    playerGauge += playerPower;
                    player2Gauge -= playerPower;
                    out.println("PLAYER:" + playerPower);
                    updateGaugePanel();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftKeyPressed = false;
                }
            }
        });

        setFocusable(true);
        setVisible(true);

        JOptionPane.showMessageDialog(this, "Shift 키를 빠르게 눌러 상대방의 팔을 넘기세요!");

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("isPlayer1")) {
                        isPlayer1 = true;
                    } else if (message.startsWith("PLAYER2:")) {
                        player2Name = message.substring(8);
                        player2Label.setText(player2Name);
                        startGameSetup();
                    } else if (message.startsWith("PLAYER:")) {
                        player2Power = Integer.parseInt(message.substring(7));
                        playerGauge -= player2Power;
                        player2Gauge += player2Power;
                        updateGaugePanel();
                    } else if (message.startsWith("START")) {
                        String[] parts = message.split(":");
                        int gameTime = Integer.parseInt(parts[1]);
                        out.println("START:" + gameTime);
                        countdownAndStartGame(gameTime);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void submitName() {
        playerName = playerNameField.getText();
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.");
            return;
        }
        playerLabel.setText(playerName);
        out.println(playerName);
    }

    private void startGameSetup() {
        if (player2Name == null || playerName == null) {
            JOptionPane.showMessageDialog(this, "상대방의 이름을 기다리고 있습니다...");
            return;
        }

        if (isPlayer1) {
            String[] options = {"10초", "20초", "30초"};
            int choice = JOptionPane.showOptionDialog(this, "게임 시간을 선택하세요:", "게임 시간 선택", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            int gameTime;
            switch (choice) {
                case 0:
                    gameTime = 10;
                    break;
                case 1:
                    gameTime = 20;
                    break;
                case 2:
                    gameTime = 30;
                    break;
                default:
                    return;
            }
            out.println("START:" + gameTime);
        } else {
            JOptionPane.showMessageDialog(this, "상대방이 시간을 설정하는 중입니다...");
        }
    }

    private void countdownAndStartGame(int gameTime) {
        SwingUtilities.invokeLater(() -> {
            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "gamePanel");
        });

        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    int finalI = i;
                    SwingUtilities.invokeLater(() -> timeLabel.setText("게임 시작까지: " + finalI + "초"));
                    Thread.sleep(1000);
                }
                SwingUtilities.invokeLater(() -> {
                    gameRunning = true;
                    timeLabel.setText("남은 시간: " + gameTime + "초");
                });
                for (int i = gameTime; i >= 0; i--) {
                    int finalI = i;
                    SwingUtilities.invokeLater(() -> timeLabel.setText("남은 시간: " + finalI + "초"));
                    Thread.sleep(1000);
                }
                endGame();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void updateGaugePanel() {
        playerPanel.setPreferredSize(new Dimension(playerGauge, 40));
        player2Panel.setPreferredSize(new Dimension(player2Gauge, 40));
        gaugePanel.revalidate();
        gaugePanel.repaint();
    }

    private void endGame() {
        if (!gameRunning) return; // 게임이 이미 종료되었다면 더 이상 처리하지 않음
        gameRunning = false;
        GameLogic gameLogic = new GameLogic(playerName, player2Name);
        String winner = gameLogic.determineWinner(playerGauge, player2Gauge);
        JOptionPane.showMessageDialog(this, "승자: " + winner + ". 축하합니다!\n다시 승부가 시작됩니다.");

        matchHistory.add("승자: " + winner);
        matchHistoryModel.addElement("승자: " + winner);

        if (isPlayer1) {
            int choice = JOptionPane.showOptionDialog(this, "게임을 다시 시작하시겠습니까?", "게임 다시 시작", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"다시 시작", "종료"}, "다시 시작");
            if (choice == JOptionPane.YES_OPTION) {
                out.println("RESTART");
            } else {
                out.println("END");
                System.exit(0);
            }
        }
    }

    private void generateRandomKeys() {
        char[] keys = "BCDEFGHIJLMNOPQRSTUVWXYZ".toCharArray();
        player1RandomKey = keys[random.nextInt(keys.length)];
        player2RandomKey = keys[random.nextInt(keys.length)];
        System.out.println("Player 1 Boost key: " + player1RandomKey);
        System.out.println("Player 2 Boost key: " + player2RandomKey);

        if (player1RandomKeyLabel != null && player2RandomKeyLabel != null) {
            player1RandomKeyLabel.setText("1P 파워 부스트 키: " + player1RandomKey);
            player2RandomKeyLabel.setText("2P 파워 부스트 키: " + player2RandomKey);
        } else {
            System.err.println("player1RandomKeyLabel 또는 player2RandomKeyLabel이 null입니다.");
        }
        player1PowerBoostActive = true;
        player2PowerBoostActive = true;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new GameFrame();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
