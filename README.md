
Environments
============

| Env   |   API                              | UI                                           |
| ----  | ---------------------------------- | -------------------------------------------- |
| Dev   | https://vps4.api.dev-godaddy.com/  | https://vps4.myh.dev-godaddy.com/            |
| Test  | https://vps4.api.test-godaddy.com/ | https://vps4.myh.test-godaddy.com/           |
| Stage | https://vps4.api.stg-godaddy.com/  | https://vps4.myh.stg-godaddy.com/            |
| Prod  | https://vps4.api.iad2.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |
| Prod-PHX | https://vps4.api.phx3.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |
| Prod-SG2 | https://vps4.api.sin2.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |

Note: Stage HUI apparently points at prod API, so (for now) 'prod' HUI is only visible through stage


Building
========

    -Pphase2-tests # activate 'phase2-tests' profile
    
    -Prebuild-db   # activate 'rebuild-db' profile



Versioning
==========

Maven uses a standard for versioning:  http://www.mojohaus.org/versions-maven-plugin/version-rules.html

    <MajorVersion [> . <MinorVersion [> . <IncrementalVersion ] ] [> - <BuildNumber | Qualifier ]>

Major.Minor.Incremental-JenkinsBuildNumber-GitCommitFirst7


TODO: move this into scripts/release?  (in root project directory?) vs storing steps in Jenkins?
** everything on Jenkins should be able to be done locally => do it

    mvn build-helper:parse-version versions:set  \
                     -DgroupId=com.godaddy.vps4  \
                     -DartifactId=vps4-parent    \
                     -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER} \
                     -DgenerateBackupPoms=false

** (finalize properties?  resolve version ranges?  all the pre-release stuff...)

To just run unit tests:

    mvn clean package

To run integration/phase2 tests (after all other build phases):

    mvn clean verify -Pphase2-tests
    
The database can be rebuilt with the 'rebuild-database' profile, which
runs in the pre-integration-test build phase:

    mvn clean verify -Pphase2-tests -Prebuild-database

    
Integration Testing
===================

Running the maven build on Jenkins:

Jenkins Integration Database:  p3dlvps4dbint.cloud.phx3.gdg

mvn clean verify     \
  -Dvps4.env=jenkins \
  -Ddb.vps4.database=db.vps4.database=vps4_int_${GIT_COMMIT} \
  -Pphase2-tests -Prebuild-database


Rebuilding Database
===================

To recreate the (empty) database and setup the vps4 database user (from the 'core' project)

    mvn initialize sql:execute@drop-create-database -Prebuild-database
    
Apply migrations:  (from an empty database -- will fail against non-empty schemas)

    mvn initialize flyway:migrate
    
If you have a database, but just need to blow away the database contents:

    mvn initialize flyway:clean


Database Migrations
===================

Migrations live in `core/sql/migrations`

Migrations must be named in the form `V{version}__{name}.sql`

 * the letter V
 * a version (like 1.2.3)
 * double-underscore
 * name
 *.sql


ZooKeeper Configuration
=======================

    mvn exec:java@zk-init -Dvps4.env={environment} -Dhfs.zk.hosts=somehostzk01.cloud.phx3.gdg,somehostzk02.cloud.phx3.gdg

'vps4.env' is the configuration at core/src/main/resources/com/godaddy/vps4/config/{vps4.env}
that will override the base configuration at core/src/main/resources/com/godaddy/vps4/config/base

'hfs.zk.hosts' is the ZooKeeper cluster that will be written to



Encrypted Configuration
=======================
First, you need to update your Java files to allow unlimited strength encryption. Download and follow
the directions in the readme.txt here: 
http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html

Configuration files are kept in core/src/main/resources/{environment}/

`config.properties` contains all the properties for that particular environment

Secrets like, for instance, production database credentials, are stored in an encrypted
properties file in the environment directory:

`config.properties.enc` is a properties file encrypted with the environment's public key

The keys for the respective environments are read from https://github.secureserver.net/vps4/vps4/tree/master/core/src/main/resources:

    vps4.{environment}.key
    
The keys for stage and prod can be found on the Jenkins server at /opt/vps4/keys. 
_Never_ check in the `vps4.stage.key` or `vps4.prod.key` files.
These files are explicitly ignored in .gitignore.

To modify encrypted properties, unencrypt the `vps.properties.enc` file for a particular environment: 

    mvn -Dvps4.env={environment} exec:java@decrypt-config

This will use the environment private key to decrypt `vps4.properties.enc` into `vps4.properties.unenc`.

Modify `vps4.properties.unenc`, then re-encrypt:

    mvn -Dvps4.env={environment} exec:java@encrypt-config


_Never_ check in the `vps4.properties.unenc` files, as these contain the plaintext secrets.
These files are explicitly ignored in .gitignore.


Creating VM Test Credits
=======================
Oh, you want to test creating a VM?  You'll need a VM credit for that.

You can create a credit via the following link:
 - http://intl.api.int.godaddy.com/ItemPurchase/Purchase.htm
 - Enter desired test environment: dev or test (sorry can't use in prod)
 - Enter shopperId and password
 - Enter the pfid for the vps4 product you want followed by any of the pfids for any addon features you want (Follow the examples)
   - Confluence link for current pfids:  https://confluence.godaddy.com/pages/viewpage.action?pageId=58980006
   - Examples 
     - {1066863} creates a linux prime container with no add ons.
     - {1066863:1068243} creates a linux prime container with a control panel
     - {1066863:1068243|1068247} creates a linux prime container with a control panel and monitoring
     
Cancelling VM Credits
======================
Use the cancellation service to initiate VPS4 account removal
https://confluence.godaddy.com/pages/viewpage.action?spaceKey=EPW&title=Cancellation+SOAP+API

```
Vicki McAtee [2:40 PM] 
you shouldn't remove an account via oat
call the cancellation service
it will remove the billing and flow through to orion and create events to you
```   

Once the Orion event is created, the HFS orion listener will be notified and will create a remove message in the appropriate kafka environement for the VPS4 Message Consumer to consume.  

Scheduler setup
=======================
To build a database to setup the jobs for automated / on-demand backup, patching or for support user related things, run the following command on your localhost from the core project module.

    mvn initialize sql:execute@drop-create-scheduler-database sql:execute@create_scheduler_tables -Prebuild-scheduler-db

Scheduler Commands in VPS4 Swagger
=======================
The scheduler commands are only available with certificate authentication.  You will need the vps4_end_web_developer.p12 in your browser to authenticate these commands.

Links

| Env     |   URL                                             |
| ------  | ------------------------------------------------- |
| Dev     | https://vps4-cca.api.dev-godaddy.com/#/scheduler  |
| Test    | https://vps4-cca.api.test-godaddy.com/#/scheduler |
| Stage   | https://vps4-cca.api.stg-godaddy.com/#/scheduler  |
| Prod-A2 | https://vps4-cca.api.iad2.godaddy.com/#/scheduler |
| Prod-PHX| https://vps4-cca.api.phx3.godaddy.com/#/scheduler |
| Prod-SG2| https://vps4-cca.api.sin2.godaddy.com/#/scheduler |
   
CRM Links
==============

| Env   |   API                              |
| ----  | ---------------------------------- |
| Dev   | https://crm.int.dev-godaddy.com    |
| Test  | https://crm.int.test-godaddy.com   |
| Stage | unknown                            |
| Prod  | https://crm.int.godaddy.com        |

Local Environment
==================

To start the vps4 web application in local environment 
without using clustered orchestration engine, 
specify the following property 
in your local IDE run configuration using the -D flag. 

    orchestration.engine.clustered=false 

Orchestration Commands in VPS4 Swagger
==================

The orchestration api is exposed through the vps4 swagger in the "commands" section.  To authenticate the for the commands api calls, you must impersonate any shopper.  The commands are set to EmployeeOnly level impersonation.

Things to note:
 - the possible queue names for /api/queue/{queueName} are "active" and "failed"
 - POST: /api/commands{commandId}/enqueue manually retries a command.
 - Failed commands will automatically retry on a scheduled interval.
 
 Finding App Servers in MCP
==================
Run this command to get a list of the current app servers, and what version of vps4 they're running.

| Env     |   Command                                         |
| ------  | ------------------------------------------------- |
| Dev     | ssh p3dlvps4mcp.cloud.phx3.gdg "mcpctl status"    |
| Test    | ssh p3tlvps4mcp.cloud.phx3.gdg "mcpctl status"    |
| Stage   | ssh p3slvps4mcp.cloud.phx3.gdg "mcpctl status"    |
| Prod-A2 | ssh a2plvps4mcp.cloud.iad2.gdg "mcpctl status"    |
| Prod-PHX| ssh p3plvps4mcp.cloud.phx3.gdg "mcpctl status"    |
| Prod-SG2| ssh sg2plvps4mcp.cloud.sin2.gdg "mcpctl status"   |

Application Monitoring
==================

The application monitoring api is exposed through the vps4 swagger in the "appmonitors" section. The commands are set to EmployeeOnly level impersonation.
The appropriate SSL certificates will need to be placed in the directory where the script is set to run. 
[TBD] At the moment, we have set it up under the user `abhoite` but should soon be moved to a different `<vps4_user>`. 
The monitoring scripts are setup manually and invoked using a cron scheduler on a separate server in each DC for the production environment.

| DC Env    |   Server                              |
| -------   | ------------------------------------- |
| P3 Prod   | p3plvps4rprt01.cloud.phx3.gdg         |
| A2 Prod   | TBD                                   |
| SG2 Prod  | TBD                                   |

Process to setup a cron job:
* Edit the crontab and ensure the jobs are entered as below.
    ```
    [root@p3plvps4rprt01 ~]# crontab -e
    */30 * * * * root cd /home/abhoite;/home/abhoite/monitor_prov_pending_servers_prod_p3.sh > /dev/null 2>&1
    */20 * * * * root cd /home/abhoite;/home/abhoite/monitor_start_pending_servers_prod_p3.sh > /dev/null 2>&1
    */21 * * * * root cd /home/abhoite;/home/abhoite/monitor_stop_pending_servers_prod_p3.sh > /dev/null 2>&1
    */22 * * * * root cd /home/abhoite;/home/abhoite/monitor_restart_pending_servers_prod_p3.sh > /dev/null 2>&1
    ```

* Restart the crond service.
    ```
    [root@p3plvps4rprt01 ~]# sudo systemctl restart crond.service
    ```


 
