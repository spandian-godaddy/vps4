package com.godaddy.vps4.intent.model;

public class Intent {
    public Intent() {}

    public Intent(Intent intent) {
        this.id = intent.id;
        this.name = intent.name;
        this.description = intent.description;
    }

        public Intent(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int id;
    public String name;
    public String description;
}
