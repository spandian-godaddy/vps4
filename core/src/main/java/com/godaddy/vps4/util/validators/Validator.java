package com.godaddy.vps4.util.validators;

import java.util.ArrayList;
import java.util.List;

public class Validator {

    final List<Rule> rules;

    public Validator(List<Rule> rules) {
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public boolean isValid(String value) {
        for (Rule rule : rules) {
            if (!rule.isValid(value)) {
                return false;
            }
        }
        return true;
    }

    public List<Validation> validate(String value) {
        List<Validation> validations = new ArrayList<>();
        for (Rule rule : rules) {
            validations.add(new Validation(rule, rule.isValid(value)));
        }
        return validations;
    }

}
