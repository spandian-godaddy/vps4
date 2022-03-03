package com.godaddy.vps4.phase2.customNotes;

import com.godaddy.vps4.customNotes.CustomNote;
import com.godaddy.vps4.customNotes.CustomNotesService;
import com.godaddy.vps4.customNotes.jdbc.JdbcCustomNotesService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JdbcCustomNotesTest {

    static private Injector injectorForDS;
    private Injector injector;
    static private DataSource dataSource;
    private UUID vmId;
    private UUID differentVmId;

    private Long differentVmNoteId;

    @BeforeClass
    public static void setUpInternalInjector() {
        injectorForDS = Guice.createInjector(new DatabaseModule());
        dataSource = injectorForDS.getInstance(DataSource.class);
    }

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DataSource.class).toInstance(dataSource);
                bind(CustomNotesService.class).to(JdbcCustomNotesService.class);
            }
        });
        VirtualMachine vm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
        VirtualMachine differentVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
        differentVmId = differentVm.vmId;
        vmId = vm.vmId;
        SqlTestData.insertTestCustomNotes(vmId, dataSource, "Test Note 1", "TestUser1");
        SqlTestData.insertTestCustomNotes(vmId, dataSource, "Test Note 2", "TestUser2");
        differentVmNoteId = SqlTestData.insertTestCustomNotes(differentVmId, dataSource, "Test Note Different VM", "TestUser2");
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestCustomNotes(vmId, dataSource);
        SqlTestData.cleanupTestCustomNotes(differentVmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(differentVmId, dataSource);
    }

    @Test
    public void createAndGetCustomNotesIsOk() {
        List<CustomNote> customNoteList = injector.getInstance(CustomNotesService.class).getCustomNotes(vmId);

        assertNotNull(customNoteList);
        assertEquals(2, customNoteList.size());
        assertEquals("Test Note 1", customNoteList.get(0).note);
        assertEquals("Test Note 2", customNoteList.get(1).note);
        assertEquals("TestUser1", customNoteList.get(0).author);
        assertEquals("TestUser2", customNoteList.get(1).author);
    }

    @Test
    public void getCustomNoteIsOk() {
        CustomNote customNote = injector.getInstance(CustomNotesService.class).getCustomNote(differentVmId, differentVmNoteId);

        assertNotNull(customNote);
        assertEquals("Test Note Different VM", customNote.note);
        assertEquals("TestUser2", customNote.author);
    }

    @Test
    public void getCustomNoteForWrongVmIdReturnsNull() {
        CustomNote customNote = injector.getInstance(CustomNotesService.class).getCustomNote(vmId, differentVmNoteId);
        assertNull(customNote);
    }

    @Test
    public void clearCustomNotesOk() {
        injector.getInstance(CustomNotesService.class).clearCustomNotes(vmId);
        List<CustomNote> customNoteList = injector.getInstance(CustomNotesService.class).getCustomNotes(vmId);
        assertEquals(0, customNoteList.size());
    }

    @Test
    public void deleteCustomNoteForWrongVmIdDoesNotDelete() {
        injector.getInstance(CustomNotesService.class).deleteCustomNote(vmId, differentVmNoteId);
        List<CustomNote> customNoteList = injector.getInstance(CustomNotesService.class).getCustomNotes(differentVmId);
        assertEquals(1, customNoteList.size());
    }

    @Test
    public void deleteCustomNoteOk() {
        injector.getInstance(CustomNotesService.class).deleteCustomNote(differentVmId, differentVmNoteId);
        List<CustomNote> customNoteList = injector.getInstance(CustomNotesService.class).getCustomNotes(differentVmId);
        assertEquals(0, customNoteList.size());
    }
}
