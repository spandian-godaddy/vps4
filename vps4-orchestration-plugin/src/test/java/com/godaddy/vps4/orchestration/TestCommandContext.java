package com.godaddy.vps4.orchestration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandDescriptor;
import gdg.hfs.orchestration.CommandProvider;

public class TestCommandContext implements CommandContext {

    final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    final CommandProvider commandProvider;

    public TestCommandContext(CommandProvider commandProvider) {
        this.commandProvider = commandProvider;
    }

    @Override
    public <CommandClass extends Command<Req, Res>, Req, Res>
        Res execute(Class<CommandClass> commandClass, Req request) {

        CommandDescriptor desc = CommandDescriptor.fromClass(commandClass);

        String alias = desc.getCommandName();

        return execute(alias,
            ctx -> commandProvider.provide(commandClass).execute(ctx, request));
    }

    @Override
    public <CommandClass extends Command<Req, Res>, Req, Res>
        Res execute(String alias, Class<CommandClass> commandClass, Req request) {

        return execute(alias,
            ctx -> commandProvider.provide(commandClass).execute(ctx, request));
    }

    @Override
    public <Req, Res> Res execute(String alias, Function<CommandContext, Res> f) {

        CommandContext childContext = newChildContext(alias);

        return (Res)cache.computeIfAbsent(alias, key -> f.apply(childContext));
    }

    protected CommandContext newChildContext(String alias) {
        return new TestCommandContext(commandProvider);
    }

}
