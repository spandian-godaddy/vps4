package com.godaddy.vps4.web.customNotes;

import com.godaddy.vps4.customNotes.CustomNotesService;
import com.godaddy.vps4.customNotes.jdbc.JdbcCustomNotesService;
import com.google.inject.AbstractModule;

public class CustomNotesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CustomNotesService.class).to(JdbcCustomNotesService.class);
    }
}
