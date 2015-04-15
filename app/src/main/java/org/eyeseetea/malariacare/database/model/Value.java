package org.eyeseetea.malariacare.database.model;

import com.orm.SugarRecord;

public class Value extends SugarRecord<Value> {

    Option option;
    Question question;
    String value;

    public Value() {
    }

    public Value(Option option, Question question) {
        this.option = option;
        this.question = question;
        this.value = "";
    }

    public Value(Option option, Question question, String value) {
        this.option = option;
        this.question = question;
        this.value = value;
    }

    public Option getOption() {
        return option;
    }

    public void setOption(Option option) {
        this.option = option;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Value{" +
                "id='" + id + '\'' +
                ", option=" + option +
                ", question=" + question +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Value value = (Value) o;

        if (option != null ? !option.equals(value.option) : value.option != null) return false;
        if (question != null ? !question.equals(value.question) : value.question != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = option != null ? option.hashCode() : 0;
        result = 31 * result + (question != null ? question.hashCode() : 0);
        return result;
    }
}