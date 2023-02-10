# Intellij Run Configs
Details on setting up run config(s) in IntelliJ to run various vps4 microservices as well as maven tasks

### Message consumer (run locally)
```
Type: Application (in Intellij run config editor)
Main class: com.godaddy.vps4.consumer.Vps4ConsumerApplication
VM options: -DSkipZkRegistration=true -Dorchestration.engine.clustered=false -Dvps4.web.useJwtAuth=true
Working directory: {PATH_TO_VPS4_SOURCE}/vps4-message-consumer
Use class path of module: vps4-message-consumer
```


### Scheduler memory mode
```
Type: Application (in Intellij run config editor)
Main class: com.godaddy.vps4.scheduler.Vps4SchedulerMain
VM options: -Dscheduler.jobstore.mode=memory -Dscheduler.useJwtAuth=true -Dscheduler.useMutualAuth=false
Working directory: {PATH_TO_VPS4_SOURCE}/vps4-scheduler
Use class path of module: vps4-scheduler
```


### Scheduler JDBC mode
```
Type: Application (in Intellij run config editor)
Main class: com.godaddy.vps4.scheduler.Vps4SchedulerMain
VM options: -Dscheduler.jobstore.mode=jdbc -Dscheduler.useJwtAuth=true -Dscheduler.useMutualAuth=false
Working directory: {PATH_TO_VPS4_SOURCE}/vps4-scheduler
Use class path of module: vps4-scheduler
```


### Vps4 API
```
Type: Application (in Intellij run config editor)
Main class: com.godaddy.vps4.web.Vps4Application
VM options: -Dorchestration.engine.clustered=false
Working directory: {PATH_TO_VPS4_SOURCE}/web
Use class path of module: vps4-web
```


### Orchestration (Non-Plugin)
```
Type: Application (in Intellij run config editor)
Main class: gdg.hfs.orchestration.web.OrchestrationWebApplication
VM options: -Dorchestration.engine.mode=memory -Dhfs.http.port=8088
Working directory: {PATH_TO_VPS4_SOURCE}/vps4-orchestration-runner
Use class path of module: vps4-orchestration-runner
```


### Orchestration (Plugin)
```
Type: Application (in Intellij run config editor)
Main class: gdg.hfs.orchestration.web.OrchestrationWebApplication
VM options: -Dorchestration.engine.mode=memory -Dhfs.http.port=8088 -Dorchestration.plugins.path={PATH_TO_VPS4_SOURCE}/vps4-orchestration-plugin/target
Working directory: {PATH_TO_VPS4_SOURCE}/vps4-orchestration-runner
Use class path of module: vps4-orchestration-runner
```


### Maven Encrypt Config
```
Type: Maven (in Intellij run config editor)
Working directory: {PATH_TO_VPS4_SOURCE}/core
Command line: -Dvps4.env=stage exec:java@encrypt-config
Profiles: NA
```

### Maven Decrypt Config
```
Type: Maven (in Intellij run config editor)
Working directory: {PATH_TO_VPS4_SOURCE}/core
Command line: -Dvps4.env=stage exec:java@decrypt-config
Profiles: NA
```


### Running phase 2 tests
```
Type: Maven (in Intellij run config editor)
Working directory: {PATH_TO_VPS4_SOURCE}
Command line: clean verify
Profiles: phase2-tests
```


### Maven Package but skip unit tests
```
Type: Maven (in Intellij run config editor)
Working directory: {PATH_TO_VPS4_SOURCE}
Command line: clean package
Profiles: NA
Use project settings (On 'Runner' tab): unchecked
VM Options (On 'Runner' tab): -Dmaven.test.skip=true
```
