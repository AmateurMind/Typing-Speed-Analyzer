import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class TypingSpeedCalculator extends JFrame {
    private JTextArea inputTextArea;
    private JTextPane promptTextPane;
    private JButton showScoresButton;
    private JButton restartButton;
    private JLabel timeLabel;
    private JLabel accuracyLabel;
    private JLabel speedLabel;
    private Date startTime;
    private long endTimeMillis;
    private int stopLength;
    private Timer timer;
    private String promptText;
    private int totalWordsTyped;
    private boolean testRunning;

    // Database connection parameters
    private static final String jdbcUrl = "jdbc:mysql://localhost:3306/records";
    private static final String username = "root";
    private static final String password = "password";

    public TypingSpeedCalculator() {
        setTitle("Typing Speed Master");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 245, 250));

        // Set custom font if available
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf")).deriveFont(14f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (Exception e) {
            // Use default font if custom font not available
        }

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        mainPanel.setBackground(new Color(245, 245, 250));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(245, 245, 250));
        JLabel titleLabel = new JLabel("Typing Speed Master", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(52, 152, 219));
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Prompt section
        JPanel promptPanel = new JPanel(new BorderLayout(10, 10));
        promptPanel.setBorder(new LineBorder(new Color(189, 195, 199), 2, true));
        promptPanel.setBackground(Color.WHITE);

        promptTextPane = new JTextPane();
        promptTextPane.setText("Get ready to type...");
        promptTextPane.setFont(new Font("Consolas", Font.PLAIN, 20));
        promptTextPane.setForeground(new Color(149, 165, 166));
        promptTextPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        promptTextPane.setBackground(Color.WHITE);
        promptTextPane.setEditable(false);
        promptPanel.add(new JScrollPane(promptTextPane), BorderLayout.CENTER);
        mainPanel.add(promptPanel, BorderLayout.CENTER);

        // Input section
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBorder(new LineBorder(new Color(189, 195, 199), 2, true));
        inputPanel.setBackground(Color.WHITE);

        JLabel inputLabel = new JLabel("Start typing here:", SwingConstants.LEFT);
        inputLabel.setFont(new Font("Arial", Font.BOLD, 16));
        inputLabel.setForeground(new Color(52, 73, 94));
        inputLabel.setBorder(new EmptyBorder(15, 15, 5, 15));
        inputPanel.add(inputLabel, BorderLayout.NORTH);

        inputTextArea = new JTextArea(6, 50);
        inputTextArea.setFont(new Font("Consolas", Font.PLAIN, 18));
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        inputTextArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        inputTextArea.setBackground(new Color(248, 249, 250));
        inputTextArea.setForeground(new Color(44, 62, 80));
        inputTextArea.setCaretColor(new Color(52, 152, 219));
        JScrollPane scrollPane = new JScrollPane(inputTextArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        inputPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // Stats panel
        JPanel statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBackground(new Color(245, 245, 250));
        statsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Time label
        timeLabel = new JLabel("Time: 0s", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timeLabel.setForeground(new Color(231, 76, 60));
        timeLabel.setBorder(new EmptyBorder(10, 20, 10, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        statsPanel.add(timeLabel, gbc);

        // Speed label
        speedLabel = new JLabel("Speed: 0.00 WPM", SwingConstants.CENTER);
        speedLabel.setFont(new Font("Arial", Font.BOLD, 16));
        speedLabel.setForeground(new Color(46, 204, 113));
        speedLabel.setBorder(new EmptyBorder(10, 20, 10, 20));
        gbc.gridx = 1;
        gbc.gridy = 0;
        statsPanel.add(speedLabel, gbc);

        // Accuracy label
        accuracyLabel = new JLabel("Accuracy: 100%", SwingConstants.CENTER);
        accuracyLabel.setFont(new Font("Arial", Font.BOLD, 16));
        accuracyLabel.setForeground(new Color(155, 89, 182));
        accuracyLabel.setBorder(new EmptyBorder(10, 20, 10, 20));
        gbc.gridx = 2;
        gbc.gridy = 0;
        statsPanel.add(accuracyLabel, gbc);

        mainPanel.add(statsPanel, BorderLayout.SOUTH);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 245, 250));

        showScoresButton = new JButton("📊 View Past Scores");
        showScoresButton.setFont(new Font("Arial", Font.BOLD, 14));
        showScoresButton.setBackground(new Color(52, 152, 219));
        showScoresButton.setForeground(Color.WHITE);
        showScoresButton.setFocusPainted(false);
        showScoresButton.setBorderPainted(false);
        showScoresButton.setBorder(new EmptyBorder(12, 25, 12, 25));
        showScoresButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        restartButton = new JButton("🔄 Restart Test");
        restartButton.setFont(new Font("Arial", Font.BOLD, 14));
        restartButton.setBackground(new Color(231, 76, 60));
        restartButton.setForeground(Color.WHITE);
        restartButton.setFocusPainted(false);
        restartButton.setBorderPainted(false);
        restartButton.setBorder(new EmptyBorder(12, 25, 12, 25));
        restartButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        restartButton.setEnabled(false);

        buttonPanel.add(showScoresButton);
        buttonPanel.add(restartButton);
        mainPanel.add(buttonPanel, BorderLayout.PAGE_END);

        add(mainPanel);

        // Event listeners
        inputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkInput();
            }
        });

        showScoresButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPastScores();
            }
        });

        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restartTest();
            }
        });

        // Timer for updating stats
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateElapsedTime();
            }
        });

        // Generate a random prompt when the application starts
        promptText = generateRandomPrompt();
        updatePromptDisplay();
    }

    private void updateElapsedTime() {
        if (testRunning) {
            long currentTime = new Date().getTime();
            long elapsedTime = currentTime - startTime.getTime();
            timeLabel.setText("Time: " + (elapsedTime / 1000) + "s");

            String typedText = inputTextArea.getText();
            StringTokenizer tokenizer = new StringTokenizer(typedText);
            int wordsTyped = tokenizer.countTokens();
            totalWordsTyped = wordsTyped;

            // Max time limit of 2 minutes
            if (elapsedTime >= 120000) {
                stopTypingTest();
            }

            if (totalWordsTyped > 0 && elapsedTime > 0) {
                double typingSpeed = (double) totalWordsTyped / (elapsedTime / 60000.0); // Calculate WPM

                // Calculate Accuracy
                int correctChars = 0;
                int minLen = Math.min(typedText.length(), promptText.length());
                for (int i = 0; i < minLen; i++) {
                    if (typedText.charAt(i) == promptText.charAt(i)) {
                        correctChars++;
                    }
                }

                double accuracy = typedText.isEmpty() ? 100.0 : ((double) correctChars / typedText.length()) * 100.0;

                speedLabel.setText(String.format("Speed: %.2f WPM", typingSpeed));
                accuracyLabel.setText(String.format("Accuracy: %.1f%%", accuracy));
            }
        }
    }

    private String generateRandomPrompt() {
        String[] prompts = {
                "The quick brown fox jumps over the lazy dog.",
                "The only way to do great work is to love what you do.",
                "In the middle of difficulty lies opportunity.",
                "Actions speak louder than words in most situations.",
                "A journey of a thousand miles begins with a single step.",
                "Every cloud has a silver lining if you look for it.",
                "Practice makes perfect when learning a new skill.",
                "Do not count your chickens before they hatch.",
                "To be or not to be, that is the question.",
                "All that glitters is not gold.",
                "Where there is a will, there is a way.",
                "The early bird catches the worm.",
                "Two wrongs don't make a right.",
                "When in Rome, do as the Romans do.",
                "Better late than never."
        };
        Random random = new Random();
        return prompts[random.nextInt(prompts.length)];
    }

    private void checkInput() {
        String typedText = inputTextArea.getText();

        // Live character-by-character feedback
        updateHighlighting();

        // Auto-start on first input
        if (!testRunning && typedText.length() > 0) {
            startTypingTest();
        }

        // Auto-stop the instant the user reaches the last *letter* of the prompt.
        // (Ignores trailing punctuation/whitespace like '.', '!', '?', spaces, etc.)
        if (testRunning && typedText.length() >= stopLength) {
            endTimeMillis = System.currentTimeMillis();
            stopTypingTest();
        }
    }

    private void startTypingTest() {
        showScoresButton.setEnabled(false);
        restartButton.setEnabled(true);
        testRunning = true;

        startTime = new Date();
        endTimeMillis = 0;
        totalWordsTyped = 0;
        timer.start();

        // Change input area appearance when test starts
        inputTextArea.setBackground(Color.WHITE);
        inputTextArea.setForeground(new Color(44, 62, 80));
        promptTextPane.setFont(new Font("Consolas", Font.BOLD, 20));
        promptTextPane.setForeground(new Color(52, 73, 94));
    }

    private void stopTypingTest() {
        if (testRunning) {
            if (endTimeMillis <= 0) {
                endTimeMillis = System.currentTimeMillis();
            }

            timer.stop();
            inputTextArea.setEnabled(false);
            showScoresButton.setEnabled(true);
            testRunning = false;

            double speed = calculateTypingSpeed();
            saveTypingSpeed(speed);

            // Show completion message with styling
            JOptionPane.showMessageDialog(this,
                    "🎉 Test Completed! Your speed and accuracy have been recorded.\n" +
                            "Get ready for a new challenge!",
                    "Test Finished",
                    JOptionPane.INFORMATION_MESSAGE);

            // Automatically reset for another test after a short delay
            new Timer(1500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((Timer) e.getSource()).stop();
                    resetForNextTest();
                }
            }).setRepeats(false);
        }
    }

    private void resetForNextTest() {
        inputTextArea.setEnabled(true);
        inputTextArea.setText("");
        inputTextArea.setBackground(new Color(248, 249, 250));
        promptText = generateRandomPrompt();
        updatePromptDisplay();
        promptTextPane.setFont(new Font("Consolas", Font.ITALIC, 20));
        promptTextPane.setForeground(new Color(149, 165, 166));
        timeLabel.setText("Time: 0s");
        speedLabel.setText("Speed: 0.00 WPM");
        accuracyLabel.setText("Accuracy: 100%");
        showScoresButton.setEnabled(false);
        restartButton.setEnabled(true);
    }

    private void restartTest() {
        resetForNextTest();
    }

    private void updatePromptDisplay() {
        promptTextPane.setText(promptText);
        stopLength = calculateStopLength(promptText);
        resetHighlighting();
    }

    private int calculateStopLength(String text) {
        // Stop at the last letter/digit (ignore trailing punctuation/whitespace)
        int i = text.length();
        while (i > 0) {
            char c = text.charAt(i - 1);
            if (Character.isLetterOrDigit(c)) {
                break;
            }
            i--;
        }
        return i;
    }

    private void resetHighlighting() {
        StyledDocument doc = promptTextPane.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, new Color(52, 73, 94));
        doc.setCharacterAttributes(0, promptText.length(), normal, true);
    }

    private void updateHighlighting() {
        String typedText = inputTextArea.getText();
        StyledDocument doc = promptTextPane.getStyledDocument();

        // Reset all styling first
        resetHighlighting();

        // Apply highlighting based on typed text
        SimpleAttributeSet correct = new SimpleAttributeSet();
        StyleConstants.setForeground(correct, new Color(46, 204, 113)); // Green for correct

        SimpleAttributeSet wrong = new SimpleAttributeSet();
        StyleConstants.setForeground(wrong, new Color(231, 76, 60)); // Red for wrong
        StyleConstants.setStrikeThrough(wrong, true); // Strike-through for wrong

        int minLen = Math.min(typedText.length(), promptText.length());

        for (int i = 0; i < minLen; i++) {
            if (typedText.charAt(i) == promptText.charAt(i)) {
                doc.setCharacterAttributes(i, 1, correct, false);
            } else {
                doc.setCharacterAttributes(i, 1, wrong, false);
            }
        }
    }

    private double calculateTypingSpeed() {
        // Calculate typing speed based on the number of words typed and elapsed time
        long end = (endTimeMillis > 0) ? endTimeMillis : System.currentTimeMillis();
        long elapsedTime = end - startTime.getTime();
        int wordsTyped = new StringTokenizer(inputTextArea.getText()).countTokens();

        if (elapsedTime == 0) {
            return 0.0;
        }

        return (double) wordsTyped / (elapsedTime / 60000.0); // WPM calculation
    }

    private void saveTypingSpeed(double speed) {
        // Save typing speed data to the database
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                PreparedStatement preparedStatement = connection
                        .prepareStatement("INSERT INTO typing_speed (speed) VALUES (?)")) {
            preparedStatement.setDouble(1, speed);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            // Silently fail - typing functionality still works without database
            System.out.println("Database not available - score not saved: " + e.getMessage());
        }
    }

    private void showPastScores() {
        // Retrieve and display past typing speed scores from the database
        StringBuilder scores = new StringBuilder();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement
                    .executeQuery("SELECT speed, test_date FROM typing_speed ORDER BY test_date DESC LIMIT 10");

            boolean hasScores = false;
            while (resultSet.next()) {
                hasScores = true;
                double speed = resultSet.getDouble("speed");
                Date testDate = resultSet.getDate("test_date");
                scores.append(String.format("• %.2f WPM - %s%n", speed, testDate));
            }

            if (!hasScores) {
                scores.append("No scores recorded yet. Take your first test!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            scores.append(
                    "Database not connected. Make sure MySQL is running and configured.\n\nFor now, you can still practice typing!");
        }

        // Styled dialog for scores
        JTextArea scoresTextArea = new JTextArea(10, 40);
        scoresTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        scoresTextArea.setText(scores.toString());
        scoresTextArea.setEditable(false);
        scoresTextArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        scoresTextArea.setBackground(new Color(248, 249, 250));

        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        dialogPanel.add(new JLabel("📊 Your Typing Speed History", SwingConstants.CENTER), BorderLayout.NORTH);
        dialogPanel.add(new JScrollPane(scoresTextArea), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, dialogPanel, "Past Scores", JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TypingSpeedCalculator calculator = new TypingSpeedCalculator();
                calculator.setVisible(true);
            }
        });
    }
}