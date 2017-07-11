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
        validators.put("hostname", getHostnameValidator());
        validators.put("snapshot-name", getSnapshotNameValidator());
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
    
    static Validator getHostnameValidator() {
        return(new Validator(Arrays.asList(
            new Rule("Fully Qualified Hostname (xxx.xxx.xxx)", "[a-zA-Z0-9-]{1,15}\\.[a-zA-Z0-9-]{1,15}\\.[a-zA-Z0-9-]{1,15}"),
            new Rule(". and - are the only allowed special characters", "^[a-zA-Z0-9-.]*$"),
            new Rule("Less than 16 characters per section", "[a-zA-Z0-9-]{1,15}\\.[a-zA-Z0-9-]{1,15}\\.[a-zA-Z0-9-]{1,15}"),
            new Rule("Doesn't begin with a hyphen", "^(?!-).*$"),
            new Rule("Doesn't end with a hyphen", ".*(?<!-)$"),
            new Rule("Multiple periods may not be adjacent", "^((?!\\.\\.).)*$"),
            new Rule("Multiple hyphens may not be adjacent", "^((?!\\-\\-).)*$"),
            new Rule("Doesn't begin with www.", "^(?!www\\.).*$")
        )));
    }

    static Validator getSnapshotNameValidator() {
        return (new Validator(Arrays.asList(
                new Rule("Only Alpha-Numeric Values and _ and - characters", "[a-z0-9_-]*"),
                new Rule("Between 5 and 16 characters", ".{5,16}")
        )));
    }

    public static Map<String, Validator> getInstance() {
        return registry;
    }

}
