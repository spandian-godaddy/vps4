This main purpose of this module is to allow orchestration engine to be run locally (for local development) without having to mess around with the jvm classpath.


Orchestration engine when run can be configured to pickup plugin commands found either on the jvm classpath or those found in a jar. The 'IDE run configs' for the these options are listed below.
**NOTE**: some of the options in the run config may be named differently and/or optional depending on the IDE


1 **Classpath**: To have orchestration engine pickup the vps4 plugin commands from the vps4-orchestration-plugin module (i.e. via the classpath) the run config should be setup with the parameters below.

```
    Main Class: gdg.hfs.orchestration.web.OrchestrationWebApplication
    VM Options: -Dorchestration.engine.mode=memory
                -Dhfs.http.port=8088
    Working Directory: {PATH_TO_VPS4_SOURCE}/vps4-orchestration-runner 
    Use classpath of module: vps4-orchestration-runner
```


2 **Plugin jar**: To have orchestration engine pickup the vps4 plugin commands from the vps4-orchestration-plugin jar the run config should be setup with the parameters below.

**NOTE**: Typically when the maven package (_mvn clean package)_ phase is run it'll create an unshaded initial jar and a shaded uber jar in the target folder of the vps4-orchestration-plugin module. Remember to delete the unshaded jar before running the orch engine that has been configured to pickup vps4 commands from a jar.

```
    Main Class: gdg.hfs.orchestration.web.OrchestrationWebApplication
    VM Options: -Dorchestration.engine.mode=memory
                -Dhfs.http.port=8088
                -Dorchestration.plugins.path={PATH_TO_VPS4_SOURCE}/vps4-orchestration-plugin/target 
    Working Directory: {PATH_TO_VPS4_SOURCE}/vps4-orchestration-runner 
    Use classpath of module: vps4-orchestration-runner
```

**FOR ECLIPSE USERS:**
Eclipse doesn't recognise this module as a java project hence doesnt allow creation of a run-configuration using this module.
To fix this, create 2 files with the following content in the vps4-orchestration-runner folder. Once these files are in place, eclipse should allow creation of the above run-configs.
**NOTE**: Do not check in the below 2 files into source control as these files are specific to only Eclipse. 

1 _.project_ file
```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>vps4-orchestration-runner</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.m2e.core.maven2Builder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
        <nature>org.eclipse.jdt.core.javanature</nature>
        <nature>org.eclipse.m2e.core.maven2Nature</nature>
	</natures>
</projectDescription>
```

2 .classpath file 
```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8">
		<attributes>
			<attribute name="maven.pomderived" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="con" path="org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER">
		<attributes>
			<attribute name="maven.pomderived" value="true"/>
		</attributes>
	</classpathentry>
</classpath>
```
