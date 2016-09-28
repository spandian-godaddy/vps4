package com.godaddy.vps4.util.validators;

public class Validation {

    public final Rule rule;

    public final boolean valid;

    public Validation(Rule rule, boolean valid) {
        this.rule = rule;
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "Validation [rule=" + rule + ", valid=" + valid + "]";
    }

}
