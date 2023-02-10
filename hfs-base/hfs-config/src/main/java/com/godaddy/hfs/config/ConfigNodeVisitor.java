package com.godaddy.hfs.config;

@FunctionalInterface
public interface ConfigNodeVisitor {

    ConfigNode visit(ConfigNode node);
}
