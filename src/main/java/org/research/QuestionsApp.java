package org.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    Color backgroundColor = new Color(245, 245, 245);  // Light gray background
    Color panelColor = new Color(255, 255, 255);        // White panel color
    Color buttonColor = new Color(0, 102, 204);         // Blue button color
    Color buttonTextColor = Color.WHITE;                // White button text
    Font defaultFont = new Font("SansSerif", Font.PLAIN, 18);  // Default font

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

        // Set global background color for the frame
        frame.getContentPane().setBackground(backgroundColor);

        // Set default font for mainPanel and its components
        mainPanel.setFont(defaultFont);
        mainPanel.setBackground(backgroundColor);

        // Prompt for login code
        promptForLogin();

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void promptForLogin() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBackground(panelColor);
        loginPanel.setBorder(new EmptyBorder(20, 20, 20, 20)); // Add padding around the panel

        JLabel label = new JLabel("Enter your login code:");
        label.setFont(defaultFont);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField codeField = new JTextField(10);
        codeField.setFont(defaultFont);
        codeField.setMaximumSize(new Dimension(200, 30)); // Limit width and height
        codeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loginButton = new JButton("Login");
        loginButton.setFont(defaultFont);
        loginButton.setBackground(buttonColor);
        loginButton.setForeground(buttonTextColor);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        // Add some spacing between components
        loginPanel.add(Box.createVerticalStrut(10));  // Vertical space
        loginPanel.add(label);
        loginPanel.add(Box.createVerticalStrut(10));  // Vertical space
        loginPanel.add(codeField);
        loginPanel.add(Box.createVerticalStrut(10));  // Vertical space
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
            URL url = new URL("https://script.google.com/macros/s/AKfycby0_nRLiRWubekH_zaFsT-oSpcvOFKrkrmw0OpRTkm1gmJ85bzu5qkm7Z2nOOGQeTRi/exec?code=" + code);
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
        packetPanel.setBackground(backgroundColor);
        packetPanel.setBorder(new EmptyBorder(20, 20, 20, 20));  // Padding

        for (String packet : exercisePackets) {
            JButton packetButton = new JButton(packet);
            packetButton.setFont(defaultFont);
            packetButton.setBackground(buttonColor);
            packetButton.setForeground(buttonTextColor);
            packetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            packetButton.setMaximumSize(new Dimension(300, 40));  // Button size

            packetButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Do you really want to start the exercise: " + packet + "?",
                        "Confirm Start",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    startExercise(packet);
                }
            });
            packetPanel.add(Box.createVerticalStrut(10));  // Add space between buttons
            packetPanel.add(packetButton);
        }

        mainPanel.add(new JScrollPane(packetPanel), BorderLayout.CENTER);
        mainPanel.updateUI();
        mainPanel.revalidate();
        mainPanel.repaint();
    }


    private void startExercise(String packetName) {
        mainPanel.removeAll();

        // Main panel to hold ChatGPT and Question sections
        JPanel exercisePanel = new JPanel(new GridLayout(1, 2, 20, 20));  // 20px gap between the two panels
        exercisePanel.setBorder(new EmptyBorder(20, 20, 20, 20));         // Margin from the edges
        exercisePanel.setBackground(backgroundColor);

        // ---- Left panel: ChatGPT interface ----
        JPanel chatPanel = new JPanel(new BorderLayout(10, 10)); // 10px padding
        chatPanel.setBackground(panelColor);
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));    // Padding inside the chat panel

        // Chat area (output)
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(defaultFont);
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));  // Padding inside the chat area
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        // Chat input (for typing questions)
        JTextField chatInput = new JTextField();
        chatInput.setFont(defaultFont);
        chatInput.setPreferredSize(new Dimension(300, 40));   // Preferred size for input

        // Send button
        JButton sendChatButton = new JButton("Send");
        sendChatButton.setFont(defaultFont);
        sendChatButton.setBackground(buttonColor);
        sendChatButton.setForeground(buttonTextColor);

        sendChatButton.addActionListener(e -> {
            String userInput = chatInput.getText();
            chatArea.append("You: " + userInput + "\n");
            chatInput.setText("");

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

        // Assemble chat panel
        JPanel chatInputPanel = new JPanel(new BorderLayout(10, 10));
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        // ---- Right panel: Question section ----
        JPanel questionPanel = new JPanel(new BorderLayout(10, 10));
        questionPanel.setBackground(panelColor);
        questionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));  // Padding around the question section

        // Question area (to display the question text)
        JTextArea questionArea = new JTextArea();
        questionArea.setEditable(false);
        questionArea.setFont(defaultFont);
        questionArea.setBorder(new EmptyBorder(10, 10, 10, 10));  // Padding inside the question area
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);

        // Panel for radio button options
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));  // 5px gap between radio buttons
        optionsPanel.setBackground(panelColor);

        ButtonGroup optionsGroup = new ButtonGroup();  // Grouping radio buttons

        // Next button to go to the next question
        JButton nextButton = new JButton("Next");
        nextButton.setFont(defaultFont);
        nextButton.setBackground(buttonColor);
        nextButton.setForeground(buttonTextColor);

        nextButton.addActionListener(e -> submitAndLoadNextQuestion(packetName, questionArea, optionsPanel, optionsGroup));

        // Assemble question panel
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.NORTH);
        questionPanel.add(optionsPanel, BorderLayout.CENTER);  // Add the radio button options panel
        questionPanel.add(nextButton, BorderLayout.SOUTH);     // Add "Next" button below the options

        // Add both panels (Chat and Question) to the main panel
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

        // Get the current question and whether the answer is correct
        Question currentQuestion = questionsApi.getQuestion(packetName, questionIndex);
        String selectedAnswer = currentQuestion.getAlternatives().get(selectedAnswerIndex);
        boolean isCorrect = selectedAnswerIndex == currentQuestion.getCorrectAnswerIndex();
        System.out.println(selectedAnswerIndex + "   " + currentQuestion.getCorrectAnswerIndex());

        // Submit selected answer and timing to Google Sheets
        questionsApi.submitAnswerToGoogleSheet(packetName, loggedInUser, questionIndex, selectedAnswer, isCorrect, timeTaken);

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
