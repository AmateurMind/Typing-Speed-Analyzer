import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class TypingSpeedCalculator extends JFrame {
    private JTextArea inputTextArea;
    private JButton startButton, stopButton, showScoresButton;
    private JLabel promptLabel, timeLabel, resultLabel;
    private Date startTime;
    private Timer timer;
    private String promptText;
    private int totalWordsTyped;
    private boolean testRunning;

    // Database connection parameters
    private static final String jdbcUrl = "jdbc:mysql://localhost:3306/records";
    private static final String username = "root";
    private static final String password = "mypassword";

    public TypingSpeedCalculator() {
        setTitle("Typing Speed Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        promptLabel = new JLabel("Type the following text:");
        inputTextArea = new JTextArea(10, 40);
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        showScoresButton = new JButton("Show Past Scores");
        timeLabel = new JLabel("Time: ");
        resultLabel = new JLabel("Typing Speed: ");
        testRunning = false;

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(promptLabel, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(inputTextArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(showScoresButton);

        add(topPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(timeLabel, BorderLayout.WEST);
        add(resultLabel, BorderLayout.SOUTH);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startTypingTest();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopTypingTest();
            }
        });

        showScoresButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPastScores();
            }
        });

        pack();
        setLocationRelativeTo(null);

        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateElapsedTime();
            }
        });

        // Generate a random prompt when the application starts
        promptText = generateRandomPrompt();
        promptLabel.setText(promptText);
    }
    private void updateElapsedTime() {
        if (testRunning) {
            long currentTime = new Date().getTime();
            long elapsedTime = currentTime - startTime.getTime();
            timeLabel.setText("Time: " + (elapsedTime / 1000) + " seconds");
    
            String typedText = inputTextArea.getText();
            StringTokenizer tokenizer = new StringTokenizer(typedText);
            int wordsTyped = tokenizer.countTokens();
            totalWordsTyped = wordsTyped;
    
            if (elapsedTime >= 15000) {
                stopTypingTest();
            }
    
            if (totalWordsTyped > 0 && elapsedTime > 0) {
                double typingSpeed = (double) totalWordsTyped / (elapsedTime / 60000.0); // Calculate WPM
                resultLabel.setText("Typing Speed: " + String.format("%.2f", typingSpeed) + " WPM");
            }
    
            if (typedText.equals(promptText)) {
                stopTypingTest();
            }
        }
    }
    
    // Inside your constructor, update the timer ActionListener as follows:

    private String generateRandomPrompt() {
        String[] prompts = {
            "The quick brown fox jumps over the lazy dog.",
            "The only way to do great work is to love what you do.",
            "In the middle of difficulty lies opportunity.",
            "Success is not final, failure is not fatal: It is the courage to continue that counts.",
            "To be yourself in a world that is constantly trying to make you something else is the greatest accomplishment."
        };
        Random random = new Random();
        return prompts[random.nextInt(prompts.length)];
    }

    private void startTypingTest() {
        inputTextArea.setEnabled(true);
        inputTextArea.requestFocus();
        inputTextArea.setText("");
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        showScoresButton.setEnabled(false);
        testRunning = true;

        // Generate a random prompt
        promptText = generateRandomPrompt();
        promptLabel.setText(promptText);

        startTime = new Date();
        totalWordsTyped = 0;
        timer.start();
    }

    private void stopTypingTest() {
        if (testRunning) {
            timer.stop();
            inputTextArea.setEnabled(false);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            showScoresButton.setEnabled(true);
            testRunning = false;

            double speed = calculateTypingSpeed();
            saveTypingSpeed(speed);
        }
    }

    private double calculateTypingSpeed() {
        // Calculate typing speed based on the number of words typed and elapsed time
        long currentTime = new Date().getTime();
        long elapsedTime = currentTime - startTime.getTime();
        int wordsTyped = new StringTokenizer(inputTextArea.getText()).countTokens();

        if (elapsedTime == 0) {
            return 0.0;
        }

        return (double) wordsTyped / (elapsedTime / 60000.0); // WPM calculation
    }

    private void saveTypingSpeed(double speed) {
        // Save typing speed data to the database
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO typing_speed (speed) VALUES (?)")) {
            preparedStatement.setDouble(1, speed);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showPastScores() {
        // Retrieve and display past typing speed scores from the database
        StringBuilder scores = new StringBuilder();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT speed, test_date FROM typing_speed");

            while (resultSet.next()) {
                double speed = resultSet.getDouble("speed");
                Date testDate = resultSet.getDate("test_date");
                scores.append("Speed: ").append(String.format("%.2f", speed)).append(" WPM, Date: ").append(testDate).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JTextArea scoresTextArea = new JTextArea(10, 40);
        scoresTextArea.setText(scores.toString());
        scoresTextArea.setEditable(false);

        JOptionPane.showMessageDialog(this, new JScrollPane(scoresTextArea), "Past Typing Speed Scores", JOptionPane.PLAIN_MESSAGE);
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
