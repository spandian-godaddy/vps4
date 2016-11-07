package com.godaddy.vps4.util.validators;

import java.util.List;
import java.util.stream.Collectors;

public class Validator {

    final List<Rule> rules;

    public Validator(List<Rule> rules) {
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public boolean isValid(String value) {
        return rules.stream().allMatch(r -> r.isValid(value));
    }

    public List<Validation> validate(String value) {
        return rules.stream().map(r -> new Validation(r, r.isValid(value))).collect(Collectors.toList());
    }

}
