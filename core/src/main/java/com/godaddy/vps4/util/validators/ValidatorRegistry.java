package com.godaddy.vps4.util.validators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ValidatorRegistry {

    static final Map<String, Validator> registry = newRegistry();

    static Map<String, Validator> newRegistry() {

        Map<String, Validator> validators = new HashMap<>();
        addValidatorsToRegistry(validators);
        return validators;
    }

    static void addValidatorsToRegistry(Map<String, Validator> validators) {
        validators.put("username", getUsernameValidator());
        validators.put("password", getPasswordValidator());
    }

    static Validator getUsernameValidator() {
        return (new Validator(Arrays.asList(
            new Rule("Only Alpha-Numeric Values and _ and - characters", "[a-z0-9_-]*"),
            new Rule("Between 5 and 16 characters", ".{5,16}"),
            new Rule("Not a reserved word", "^(?!(root|admin|administrator|users|user|system|group)$).*")
        )));
    }

    static Validator getPasswordValidator() {
        return (new Validator(Arrays.asList(
            new Rule("Between 8 and 14 characters, with no disallowed characters", "[^&?;]{8,14}"),
            new Rule("Starts with a letter", "[a-zA-Z].*"),
            new Rule("Includes at least one lowercase letter", ".*[a-z].*$"),
            new Rule("Includes at least one uppercase letter", ".*[A-Z].*$"),
            new Rule("Includes at least one digit", ".*[0-9].*$"),
            new Rule("Includes at least one special character", ".*[@!#$%].*$")
        )));
    }

    public static Map<String, Validator> getInstance() {
        return registry;
    }

}
