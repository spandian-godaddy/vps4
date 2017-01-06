package com.godaddy.vps4.orchestration.phase2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class ActionCommandTest {

    static Injector injector;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new DatabaseModule(),
                new VmModule());
    }

    @Test
    public void testFailAction() {
        ActionService actionService = injector.getInstance(ActionService.class);

        CommandContext context = Mockito.mock(CommandContext.class);

        ActionRequest request = new ActionRequest() {
            @Override
            public long getActionId() {
                return 7;
            }
        };

        ActionCommand<ActionRequest, String> command = new ActionCommand<ActionRequest, String>(actionService) {
            @Override
            protected String executeWithAction(CommandContext context, ActionRequest request) throws Exception {

                if (request.getActionId() == 7) {
                    throw new Exception("Something broke");
                }

                return "World";
            }
        };

        Throwable thrown = null;
        try {
            command.execute(context, request);
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNotNull(thrown);
        Assert.assertEquals("java.lang.Exception: Something broke", thrown.getMessage());
        // we were updating a non-existant action
        // for now we just want to make sure the SQL executed successfully
    }
}
