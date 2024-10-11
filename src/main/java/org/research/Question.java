package org.research;

import java.util.List;

public class Question {
    private String text;
    private List<String> alternatives;
    private int correctAnswerIndex;

    // Getters and setters for Gson to deserialize properly
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<String> alternatives) {
        this.alternatives = alternatives;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    @Override
    public String toString() {
        return "Question{" +
                "text='" + text + '\'' +
                ", alternatives=" + alternatives +
                ", correctAnswerIndex=" + correctAnswerIndex +
                '}';
    }
}
