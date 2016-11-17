package com.godaddy.vps4.security;

import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.google.inject.AbstractModule;

// TODO refactor into SecurityModule, manage things like privileges
public class Vps4UserModule extends AbstractModule {

    @Override
    protected void configure() {
        Vps4User user = new Vps4User(2, "SomeUser");

        bind(Vps4User.class).toInstance(user);
        bind(ProjectService.class).to(JdbcProjectService.class);
    }

}