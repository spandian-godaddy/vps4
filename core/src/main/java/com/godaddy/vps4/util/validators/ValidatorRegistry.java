package com.godaddy.vps4.util.validators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ValidatorRegistry {

    static final Map<String, Validator> registry = newRegistry();

    static Map<String, Validator> newRegistry() {

        Map<String, Validator> validators = new HashMap<>();

        validators.put("username", new Validator(Arrays.asList(
            new Rule("Only Alpha-Numeric Values and _ and - characters", "[a-z0-9_-]*"),
            new Rule("Between 5 and 16 characters", ".{5,16}"),
            new Rule("Not a reserved word", "^(?!(root|admin|administrator|users|user|system|group)$).*")
        )));

        return validators;
    }

    public static Map<String, Validator> getInstance() {
        return registry;
    }

}
