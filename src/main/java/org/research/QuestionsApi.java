package org.research;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class QuestionsApi {
    private static final String API_URL = "https://ai-education-research-api.shuttleapp.rs";
    private final Gson gson;

    public QuestionsApi() {
        this.gson = new Gson();
    }

    // Get a list of available exercise packets from the API
    public String[] getAvailablePackets() {
        try {
            URL url = new URL(API_URL + "/packets");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Deserialize the JSON response into a String array using Gson
            String[] packets = gson.fromJson(response.toString(), String[].class);
            return packets;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get a specific question from a packet
    public Question getQuestion(String packetName, int questionIndex) {
        try {
            URL url = new URL(API_URL + "/packet/" + packetName + "/questions/" + questionIndex);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Deserialize the JSON response into a Question object using Gson
            Question question = gson.fromJson(response.toString(), Question.class);
            return question;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Check if more questions are available in the packet
    public boolean hasMoreQuestions(String packetName, int questionIndex) {
        try {
            // Get the packet info (including the list of questions)
            URL url = new URL(API_URL + "/packet/" + packetName + "/questions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Deserialize the response into an ExercisePacket object
            ExercisePacket packet = gson.fromJson(response.toString(), ExercisePacket.class);

            // Check if the current questionIndex is less than the number of questions
            return questionIndex < packet.getQuestions().size();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Submit the answer and timing for a question
    public void submitAnswer(String packetName, int questionIndex, String answer, long timeTaken) {
        try {
            URL url = new URL(API_URL + "/packet/" + packetName + "/questions/" + questionIndex + "/submit");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");  // Set content type to JSON
            conn.setDoOutput(true);

            // Build the JSON payload
            JsonObject jsonPayload = new JsonObject();
            jsonPayload.addProperty("answer", answer);
            jsonPayload.addProperty("timeTaken", timeTaken);

            // Write the JSON data to the output stream
            conn.getOutputStream().write(jsonPayload.toString().getBytes());

            // Get the response to ensure it went through
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            in.readLine();  // Just read the response
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
