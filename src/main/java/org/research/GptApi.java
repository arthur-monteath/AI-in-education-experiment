package org.research;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GptApi {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "";

    public void sendMessage(String message, StreamCallback callback) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            // Set timeouts (in milliseconds)
            conn.setConnectTimeout(5000); // 5 seconds
            conn.setReadTimeout(10000);    // 10 seconds

            // JSON payload with streaming enabled
            String payload = "{"
                    + "\"model\": \"gpt-4o-mini\","
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"" + message + "\"}],"
                    + "\"max_tokens\": 150,"
                    + "\"stream\": true"
                    + "}";

            // Send the payload
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read the response in parts
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    if (responseLine.startsWith("data:")) {
                        String jsonPart = responseLine.substring(5).trim(); // Remove "data: " prefix
                        if (!jsonPart.equals("[DONE]")) { // Check for end of stream signal
                            callback.onResponsePart(jsonPart); // Call the callback with the part
                        }
                    }
                }
                callback.onComplete(); // Notify completion
            }
        } catch (IOException e) {
            e.printStackTrace();
            callback.onError("Error fetching response: " + e.getMessage()); // Notify error
        }
    }
}
