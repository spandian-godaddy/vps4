package com.godaddy.vps4.util.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.godaddy.vps4.vm.Image;
import static com.godaddy.vps4.vm.Image.ControlPanel.MYH;
import static com.godaddy.vps4.vm.Image.ControlPanel.CPANEL;

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
        validators.put("cpanelHostname", getHostnameValidator(CPANEL));
        validators.put("snapshot-name", getSnapshotNameValidator());
    }

    static Validator getUsernameValidator() {
        return (new Validator(Arrays.asList(
            new Rule("Only Alpha-Numeric Values and _ and - characters", "[a-z0-9_-]*"),
            new Rule("Between 5 and 16 characters", ".{5,16}"),
            new Rule("Not a reserved word", "^(?!(" +
                    "admin|administrator|root|users|user|system|group|" +
                    "_apt|abrt|adm|avahi|avahi-autoipd|backup|bin|cpanel|cpanelcabcache|cpanelconnecttrack|" +
                    "cpaneleximfilter|cpaneleximscanner|cpanellogin|cpaneldemo|cpanelphpmyadmin|" +
                    "cpanelphppgadmin|cpanelroundcube|cpanelrrdtool|cpses|daemon|dbus|Debian-exim|dovecot|" +
                    "dovenull|ftp|games|gnats|gopher|haldaemon|halt|irc|list|lp|mail|mailman|mailnull|man|" +
                    "messagebus|mysql|named|news|nobody|nscd|ntp|nydus|operator|polkitd|postfix|proxy|root|" +
                    "rpc|saslauth|shutdown|smmsp|smmta|sshd|statd|sync|sys|syslog|systemd-bus-proxy|" +
                    "systemd-network|systemd-resolve|systemd-timesync|tcpdump|tss|uucp|uuidd|vcsa|www-data|" +
                    "cloudbase-init|psaadm)$).*")
        )));
    }

    // Non-specified controlPanel, returns default rules (MYH)
    static Validator getHostnameValidator() {
        return getHostnameValidator(MYH);
    }

    static Validator getPasswordValidator() {
        ArrayList<Rule> rules = new ArrayList<>(Arrays.asList(
            new Rule("Between 8 and 48 characters, with no disallowed characters", "[^&?;]{8,48}"),
            new Rule("Includes at least one lowercase letter", ".*[a-z].*$"),
            new Rule("Includes at least one uppercase letter", ".*[A-Z].*$"),
            new Rule("Includes at least one digit", ".*[0-9].*$"),
            new Rule("Includes at least one special character", ".*[@!#$%].*$"),
            new Rule("Not include certain special characters (& ? ; \" ')", "^[^&;?“”‘’'\"]*$"),
            new Rule("Not include spaces", "^[^\\s]*$"),
            new Rule("Not include \"admin\"", "(?i)^((?!admin).)*$"),
            new Rule("Not include international characters", "[\\x00-\\x7F]*$")
        ));
        return new Validator(rules);
    }

    static Validator getHostnameValidator(Image.ControlPanel controlPanel) {
        ArrayList<Rule> rules = new ArrayList<>((Arrays.asList(
            new Rule("Fully Qualified Hostname (xxx.xxx.xxx)", "^[^\\.]+\\.([^\\.]+\\.)+[^\\.]+$"),
            new Rule(". and - are the only allowed special characters", "^[a-zA-Z0-9-.]*$"),
            new Rule("63 characters or fewer per section", "^[^\\.]{0,63}(\\.[^\\.]{0,63})*$"),
            new Rule("No section begins with a hyphen", "^[^-.][^.]*$|^([^-.][^.]*\\.)+[^-.][^.]*$"),
            new Rule("No section ends with a hyphen", "^[^.]*[^-.]$|^([^.]*[^-.]\\.)+[^.]*[^-.]$"),
            new Rule("Multiple periods may not be adjacent", "^((?!\\.\\.).)*$"),
            new Rule("Multiple hyphens may not be adjacent", "^((?!--).)*$"),
            new Rule("Doesn't begin with www.", "^(?!www\\.).*$"),
            new Rule("Contain a non-digit in the last section", "^.*\\.[^.]*[^.0-9][^.]*$")
          )));
        if (controlPanel == CPANEL) {
            rules.add(new Rule("Doesn't begin with cpanel or whm.", "^(?!(?i)(cpanel|whm)\\.).*$"));
        }
        return new Validator(rules);
    }

    static Validator getSnapshotNameValidator() {
        return (new Validator(Arrays.asList(
                new Rule("Only Alpha-Numeric Values and _ and - characters", "[a-zA-Z0-9_-]*"),
                new Rule("Between 5 and 16 characters", ".{5,16}")
        )));
    }

    public static Map<String, Validator> getInstance() {
        return registry;
    }

}
