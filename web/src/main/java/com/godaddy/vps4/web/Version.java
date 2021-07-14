package com.godaddy.vps4.web;

public class Version {

    public static final String CURRENT = getCurrentVersion();

    private static String getCurrentVersion() {
        String version = null;

        Package pkg = Version.class.getPackage();
        if (pkg != null) {
            version = pkg.getImplementationVersion();
        }

        if (version == null) {
            version = "dev";
        }
        return version;
    }

}
