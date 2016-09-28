package com.godaddy.vps4.util.validators;

public class Rule {

    public final String description;
    public final String regex;

    public Rule(String description, String regex) {
        this.description = description;
        this.regex = regex;
    }

    public boolean isValid(String value){
        return value.matches(regex);
    }

    @Override
    public String toString() {
        return "Rule [description=" + description + ", regex=" + regex + "]";
    }

}
