package com.godaddy.hfs.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigNode {

    final List<ConfigNode> children = new ArrayList<>();

    final String name;

    final byte[] content;

    public ConfigNode(String name) {
        this(name, null);
    }

    public ConfigNode(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public List<ConfigNode> getChildren() {
        return children;
    }

    public ConfigNode rename(String newName) {
        ConfigNode newNode = new ConfigNode(newName, content);
        newNode.children.addAll(this.children);
        return newNode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, 0);
        return sb.toString();
    }

    public void toString(StringBuilder sb, int depth) {
        for (int i=0; i<depth; i++) {
            sb.append("  ");
        }
        sb.append(name);
        if (content != null) {
            sb.append("  (").append(content.length).append(" bytes)");
        }
        sb.append('\n');

        for (ConfigNode child : children) {
            child.toString(sb, depth + 1);
        }
    }

}
