
The Engine
----------

The Orchestration Engine is a server that can be submitted long-running tasks, called 'commands'.

The engine handles:
 * command state checkpointing
 * resuming command execution after a service disruption
 * command composition
 * marshalling request and response objects to/from JSON


Commands
--------

A command is just a class that implements the Command interface.

The only requirement is that it's able to be instantiated via whatever CommandProvider
is returned by your CommandPlugin.  This usually means just a default no-arg constructor
(generated automatically by the compiler if not explicitly created).

    public class ExampleCommand implements Command<Integer, String> {

        @Override
        public String execute(CommandContext context, Integer input) {
            return "You sent " + input;
        }

    }

When the Orchestration Engine receives a request to execute a particular command:

    POST /api/commands
        {
            "key": 0,
            "commands": [
                "command": "ExampleCommand",
                "request": 2
            ]
        }

It asks the CommandProvider returned by your CommandPlugin whether it has a command
named "ExampleCommand".  If it does, the engine creates an instance of the command (again,
using the CommandProvider) and calls 'execute'.

The 'request' from the POST json is mapped to the second argument of the execute method,
and the command state is persisted to the CommandStore.  The client of the POST receives
the CommandState as a response to the POST:

    {
      "id": "276c2f48-47ae-43a5-9ca9-00d32fdc3176",
      "status": "PENDING",
      "children": {
        "ExampleCommand": {
          "id": "55c3dfe3-18e8-4d38-8577-9e2fbdd83047",
          "status": "CREATED",
          "command": "ExampleCommand",
          "request": 2,
          "response": null
        }
      }
    }

Each command is assigned a UUID.  The command is immediately submitted for execution,
and will cycle through the statuses CREATED, PENDING, RUNNING, FAILED|COMPLETE.

Once complete, the 'response' field will be filled with the serialized JSON response
that was returned from the command's 'execute' method.


Plugins
-------

Commands are packaged within plugins.  Plugins are classes that implement the CommandPlugin interface,
and packaged in a jar with the commands the plugin provides.

All plugins are loaded by the Orchestration Engine on startup through the JVM ServiceLoader mechanism
(which is why the META-INF/services/gdg.hfs.orchestration.CommandPlugin file is required to be
packaged with your plugin.  Otherwise the Orchestration Engine has no way to find your plugin)


Command Composition
-------------------

    public class CommandOne implements Command<Integer, Integer> {

        @Override
        public Integer execute(CommandContext context, Integer i) {
        
            Integer j = context.execute(CommandTwo.class, i);
        
            return j + 1;
        }
    }
    
    public class CommandTwo implements Command<Integer, Integer> {

        @Override
        public Integer execute(CommandContext context, Integer i) {
            return i + 2;
        }
    }


A Command is just a Java class, and nothing stops it from calling any other code just like you
would normally.  However, if you want the result of that code to be persisted and checkpointed
you can use CommandContext.execute.

In the above example, if CommandOne was executed with a request value of 1, it would call
CommandTwo with a request value of 1.  CommandTwo would return 3, which would be stored
in the local variable 'j' in CommandOne.  CommandOne would return 4.

If we were to get the CommandState of our command, it would look like:

    {
      "id": "276c2f48-47ae-43a5-9ca9-00d32fdc3176",
      "status": "COMPLETE",
      "children": {
        "CommandOne": {
          "id": "55c3dfe3-18e8-4d38-8577-9e2fbdd83047",
          "status": "COMPLETE",
          "command": "CommandOne",
          "request": 1,
          "response": 4,
          "children": {
            "CommandTwo": {
              "id": "asdf1234-18e8-4d38-8577-9e2fbdd83047",
              "status": "COMPLETE",
              "command": "CommandTwo",
              "request": 1,
              "response": 3
            }
          }
        }
      }
    }
    

If the power plug from the server was pulled in CommandOne:

    Integer j = context.execute(CommandTwo.class, i);
    // power plug pulled here
    return j + 1;
    
CommandOne would still be IN_PROGRESS, and CommandTwo would be COMPLETE (meaning
we have a cached response for CommandTwo).

When the orchestration engine is restarted, all 'in progress' commands are
restarted.  Since CommandTwo is already complete:

    // this will return immediately
    // CommandTwo will not be executed, rather the cached
    // response from its previous successful execution
    // will be returned into 'j'
    Integer j = context.execute(CommandTwo.class, i);
    
    return j + 1;

Command Aliases
---------------

Given how the Orchestration Engine caches command execution results, this command
implementation would prove problematic:


    public class LoopingCommand implements Command<Integer, Integer> {

        @Override
        public Integer execute(CommandContext context, Integer i) {
            int j = 0;
            for (int i=0; i<10; i++) {
                j += context.execute(SomeOtherCommand.class, i);
            }
        
            return j;
        }
    }
    
In the above implementation, the first loop execution (when i==0) would execute SomeOtherCommand,
passing 0 as its argument.  All subsequent context.execute() calls would immediately return
with the previously cached value.  One way to avoid this is with aliases:


    public Integer execute(CommandContext context, Integer i) {
        
        context.execute(SomeOtherCommand.class, i);
        context.execute("SomeSpecialCall", SomeOtherCommand.class, i);
    
        return 1;
    }

The resulting CommandState would look like:

    {
      "id": "276c2f48-47ae-43a5-9ca9-00d32fdc3176",
      "status": "COMPLETE",
      "children": {
        "CommandOne": {
          "id": "55c3dfe3-18e8-4d38-8577-9e2fbdd83047",
          "status": "COMPLETE",
          "command": "CommandOne",
          "request": 1,
          "response": 4,
          "children": {
            "SomeOtherCommand": {
              "id": "asdf1234-18e8-4d38-8577-9e2fbdd83047",
              "status": "COMPLETE",
              "command": "SomeOtherCommand",
              "name": "SomeOtherCommand",
              "request": 1,
              "response": 3
            },
            "SomeSpecialCall": {
              "id": "asdf1234-18e8-4d38-8577-9e2fbdd83047",
              "status": "COMPLETE",
              "command": "SomeOtherCommand",
              "name": "SomeSpecialCall",
              "request": 1,
              "response": 3
            }
          }
        }
      }
    }
    
Notice that each command 'children' entry has a 'command' and a 'name'.
The 'command' field is the actual implementation name (derived from the class name
of the command or through the @CommandMetadata annotation) and the 'name'
is the command-local alias for that command execution.  Each local alias must be unique.
    