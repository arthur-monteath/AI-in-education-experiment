package org.research;

import java.util.List;

public class ExercisePacket {
    private String name;
    private List<Question> questions;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    @Override
    public String toString() {
        return "ExercisePacket{" +
                "name='" + name + '\'' +
                ", questions=" + questions +
                '}';
    }
}
