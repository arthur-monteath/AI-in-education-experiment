package org.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class QuestionsApp {
    private JFrame frame;
    private JPanel mainPanel;
    private QuestionsApi questionsApi;
    private GptApi gptApi;
    private String loggedInUser;  // Track the logged-in user

    private long startTime;
    private int questionIndex = 0;  // Track the current question index
    private int selectedAnswerIndex = -1;  // Track the selected answer index

    public QuestionsApp() {
        questionsApi = new QuestionsApi();
        gptApi = new GptApi();

        // Create the main window
        frame = new JFrame("GenAI Experiment");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);

        // Create the main panel layout
        mainPanel = new JPanel(new BorderLayout());

        // Prompt for login code
        promptForLogin();

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void promptForLogin() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Enter your login code:");
        JTextField codeField = new JTextField(10);
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            String code = codeField.getText();
            if (verifyLoginCode(code)) {
                JOptionPane.showMessageDialog(frame, "Login successful! Welcome " + loggedInUser);
                loadExercisePackets();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid code. Please try again.");
                codeField.setText("");
            }
        });

        loginPanel.add(label);
        loginPanel.add(codeField);
        loginPanel.add(loginButton);

        mainPanel.add(loginPanel, BorderLayout.CENTER);
        mainPanel.updateUI();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    // Verify the login code by accessing the Google Sheets API
    private boolean verifyLoginCode(String code) {
        try {
            // Example REST API endpoint that checks the code and returns a user
            // Sheet: https://docs.google.com/spreadsheets/d/1PYix9SScrZU147ztThYBSbC-LPuIRmml2gibo9RPx98/edit
            URL url = new URL("https://script.google.com/macros/s/AKfycbzdQyuRxsZP9rZyB5fMmk_s7I7sqabp_wrA9k-sXZhp7fpPjV2q_yVXLiV8FXau_FmH/exec?code=" + code);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Parse the JSON response
            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (jsonResponse.has("user")) {
                loggedInUser = jsonResponse.get("user").getAsString();  // Get the user's name
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadExercisePackets() {
        mainPanel.removeAll();
        String[] exercisePackets = questionsApi.getAvailablePackets();
        JPanel packetPanel = new JPanel();
        packetPanel.setLayout(new BoxLayout(packetPanel, BoxLayout.Y_AXIS));

        // Display the available exercise packets
        for (String packet : exercisePackets) {
            JButton packetButton = new JButton(packet);
            packetButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Do you really want to start the exercise: " + packet + "?",
                        "Confirm Start",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    startExercise(packet);
                }
            });
            packetPanel.add(packetButton);
        }

        mainPanel.add(new JScrollPane(packetPanel), BorderLayout.CENTER);
        mainPanel.updateUI();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void startExercise(String packetName) {
        mainPanel.removeAll();
        JPanel exercisePanel = new JPanel(new GridLayout(1, 2));

        // Left panel: ChatGPT interface
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JTextField chatInput = new JTextField();
        JButton sendChatButton = new JButton("Send");

        sendChatButton.addActionListener(e -> {
            String userInput = chatInput.getText();
            chatArea.append("You: " + userInput + "\n");
            chatInput.setText("");

            // Use the streaming callback for real-time updates
            gptApi.sendMessage(userInput, new StreamCallback() {
                @Override
                public void onResponsePart(String part) {
                    SwingUtilities.invokeLater(() -> chatArea.append("ChatGPT: " + part + "\n"));
                }

                @Override
                public void onComplete() {
                    SwingUtilities.invokeLater(() -> chatArea.append("ChatGPT: [response complete]\n"));
                }

                @Override
                public void onError(String errorMessage) {
                    SwingUtilities.invokeLater(() -> chatArea.append("Error: " + errorMessage + "\n"));
                }
            });
        });

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        // Right panel: Exercise questions with radio buttons for alternatives
        JPanel questionPanel = new JPanel(new BorderLayout());
        JTextArea questionArea = new JTextArea();
        questionArea.setEditable(false);
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1)); // To hold radio buttons for alternatives
        ButtonGroup optionsGroup = new ButtonGroup();  // Group the radio buttons so only one can be selected

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(e -> submitAndLoadNextQuestion(packetName, questionArea, optionsPanel, optionsGroup));

        questionPanel.add(new JScrollPane(questionArea), BorderLayout.NORTH);
        questionPanel.add(optionsPanel, BorderLayout.CENTER);  // Add the radio button options panel
        questionPanel.add(nextButton, BorderLayout.SOUTH);     // Add "Next" button below the options

        exercisePanel.add(chatPanel);
        exercisePanel.add(questionPanel);

        mainPanel.add(exercisePanel);
        mainPanel.revalidate();
        mainPanel.repaint();

        // Start the exercise with the first question
        questionIndex = 0;
        loadQuestion(packetName, questionArea, optionsPanel, optionsGroup);
    }

    // Unified method to handle both first and next questions
    private void submitAndLoadNextQuestion(String packetName, JTextArea questionArea, JPanel optionsPanel, ButtonGroup optionsGroup) {
        // Check if an answer was selected
        if (selectedAnswerIndex == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an answer.");
            return;
        }

        long timeTaken = System.currentTimeMillis() - startTime;

        // Submit selected answer (index) and timing to the API
        questionsApi.submitAnswer(packetName, questionIndex, String.valueOf(selectedAnswerIndex), timeTaken);

        // Load next question or end exercise
        questionIndex++;
        if (questionsApi.hasMoreQuestions(packetName, questionIndex)) {
            loadQuestion(packetName, questionArea, optionsPanel, optionsGroup);
            selectedAnswerIndex = -1; // Reset selected answer
        } else {
            JOptionPane.showMessageDialog(frame, "Exercise completed!");
            loadExercisePackets();  // Back to the main screen
        }
    }

    // Method to load and display a question and its alternatives
    private void loadQuestion(String packetName, JTextArea questionArea, JPanel optionsPanel, ButtonGroup optionsGroup) {
        Question currentQuestion = questionsApi.getQuestion(packetName, questionIndex);

        // Update UI with question and alternatives
        questionArea.setText(currentQuestion.getText());
        optionsPanel.removeAll();  // Clear previous options
        optionsGroup.clearSelection();

        List<String> alternatives = currentQuestion.getAlternatives();
        for (int i = 0; i < alternatives.size(); i++) {
            JRadioButton optionButton = new JRadioButton(alternatives.get(i));
            int index = i;  // Capture index for lambda
            optionButton.addActionListener(e -> selectedAnswerIndex = index);
            optionsGroup.add(optionButton);
            optionsPanel.add(optionButton);
        }

        optionsPanel.revalidate();
        optionsPanel.repaint();

        // Start timing for this question
        startTime = System.currentTimeMillis();
    }
}
