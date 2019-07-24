
Environments
=============

| Env   |   API                              | UI                                           |
| ----  | ---------------------------------- | -------------------------------------------- |
| Dev   | https://vps4.api.dev-godaddy.com/  | https://vps4.myh.dev-godaddy.com/            |
| Test  | https://vps4.api.test-godaddy.com/ | https://vps4.myh.test-godaddy.com/           |
| Stage | https://vps4.api.stg-godaddy.com/  | https://vps4.myh.stg-godaddy.com/            |
| Prod  | https://vps4.api.iad2.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |
| Prod-PHX | https://vps4.api.phx3.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |
| Prod-SG2 | https://vps4.api.sin2.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |
| Prod-AMS | https://vps4.api.ams3.godaddy.com/ | https://myh.godaddy.com/#/hosting/vps4/index |


Building
========

    -Pphase2-tests # activate 'phase2-tests' profile
    
    -Prebuild-db   # activate 'rebuild-db' profile


# Deploying

Deploying to all environments is accomplished with the VPS4 Jenkins server - https://vps4.jenkins.int.godaddy.com/

### Development

The development environment for VPS4 can be used to test your changes on a specific branch.
*Before using it, please announce in the #VPS4 slack channel that you are going to be using it and for roughly how long. When you are finished please deploy the Master branch to the Development environment and announce it in the #VPS4 channel.*

To deploy to development, execute the **vps4-dev-pipeline** Jenkins job and provide your branch name as the parameter. This will initiate the vps4-build-dev job and then dev-deploy-branch.

### Test/Stage

Test and Stage deployment are triggered by a checkin to the master branch and does not require manual interaction.

### Prod
 
Execute the **vps4-cicd** job with **PROD_DEPLOY** specified in the deployToEnv parameter, leave mcpDeploy set to enabled. 


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

Jenkins Integration Database:  p3dlvps4dbint2.cloud.phx3.gdg

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
    
The keys for stage and prod can be found on the Jenkins build slave at /opt/vps4/keys. 
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
| Prod-AMS| https://vps4-cca.api.ams3.godaddy.com/#/scheduler |
   
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
| Prod-AMS| ssh n3plvps4mcp.cloud.ams3.gdg "mcpctl status"   |

MCP Setup
============

MCP add service commands.

    $>mcpctl addservice vps4app “/service/vps4/vps4-web” vps4-web vps4-web;

    $>mcpctl addservice vps4app "/orchestration/serviceapi" vps4-orchestration-plugin hfs-engine;

    $>mcpctl addservice vps4app “/service/vps4/vps4-message-consumer“ vps4-message-consumer vps4-message-consumer;

    $>mcpctl addservice vps4app /service/vps4/vps4-scheduler vps4-scheduler vps4-scheduler;

Setup server count for each application.

    $>mcpctl count vps4-web 1;

    $>mcpctl count hfs-engine 1;

    $>mcpctl count vps4-message-consumer 1;

    $>mcpctl count vps4-scheduler 1;

Show mcp status.

    $>mcpctl status;

Cleanup failed deployments

    $>mcpctl prune

    $>mcpctl prune delete

Show all available and in use resources. (spares + in use)
    
    $>mcpctl resources

Application Monitoring
==================

The application monitoring api is exposed through the vps4 swagger in the "appmonitors" section. The commands are set to EmployeeOnly level impersonation.
The appropriate SSL certificates will need to be placed in the directory where the script is set to run. 
The jobs are setup under the user **vps4monitor** and the password is available from the vps4 development team. 
The monitoring scripts are setup manually and invoked using a cron scheduler on a separate server in each DC for the production environment.
The monitoring scripts will update the *vps4-monitoring* slack channel with alerts.

| DC Env    |   Server                              |
| -------   | ------------------------------------- |
| P3 Prod   | p3plvps4rprt01.cloud.phx3.gdg         |
| A2 Prod   | TBD                                   |
| SG2 Prod  | sg2plvps4rprt01.cloud.sin2.gdg        |
| AMS Prod  | n3plvps4rprt01.cloud.ams3.gdg        |

Process to setup a cron job:
* Edit the crontab and ensure the jobs are entered as below.
    ```
    [root@p3plvps4rprt01 ~]# crontab -e
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/backupactions?thresholdInMinutes=120 PROD P3 Backup > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/provision?thresholdInMinutes=60 PROD P3 Provision > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/restartvm?thresholdInMinutes=15 PROD P3 Restart > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/restorevm?thresholdInMinutes=120 PROD P3 Restore > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/startvm?thresholdInMinutes=15 PROD P3 Start > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/stopvm?thresholdInMinutes=15 PROD P3 Stop > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/newactions?thresholdInMinutes=120 PROD P3 All > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/missing_backup_jobs PROD P3 'Missing Backup Jobs' > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.phx3.godaddy.com/api/appmonitors/pending/allactions PROD P3 'All Pending Actions' > /dev/null 2>&1
    ```

* Restart the crond service.
    ```
    [root@p3plvps4rprt01 ~]# sudo systemctl restart crond.service
    ```

* Similarly for SG2 with different url and parameters
    ```
    [root@sg2plvps4rprt01 ~]# crontab -l
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/backupactions?thresholdInMinutes=120 PROD SG2 Backup > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/provision?thresholdInMinutes=60 PROD SG2 Provision > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/restartvm?thresholdInMinutes=15 PROD SG2 Restart > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/restorevm?thresholdInMinutes=120 PROD SG2 Restore > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/startvm?thresholdInMinutes=15 PROD SG2 Start > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/stopvm?thresholdInMinutes=15 PROD SG2 Stop > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/newactions?thresholdInMinutes=15 PROD SG2 All > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/missing_backup_jobs PROD SG2 'Missing Backup Jobs' > /dev/null 2>&1
    0 8-17 * * * root cd /home/vps4monitor;/home/vps4monitor/monitor_pending_actions.sh https://vps4-cca.api.sin2.godaddy.com/api/appmonitors/pending/allactions PROD SG2 'All Pending Actions' > /dev/null 2>&1
    ```

### Sensu Alert setup for application monitoring
Sensu Dashboard is located [here: https://vps4.sensu.prod.phx3.secureserver.net/#/events](https://vps4.sensu.prod.phx3.secureserver.net/#/events)

In addition to alerting in the slack channel, there will also be a sensu notification generated which in turn, creates a service-now ticket for any monitoring alerts in the production environment.
TODO: Eventually need to puppetize this process below.

* Install the sensu packages
    ```
      yum install -y  sensu-enterprise-plugins.noarch
      yum install -y sensu-team-plugins-vps4.noarch
    ```
* Ensure the following:
  - VPS4 sensu repository is located [here: https://vps4.sensu.prod.phx3.secureserver.net/#/checks](https://vps4.sensu.prod.phx3.secureserver.net/#/checks) 
  - To add a new check, checkout this repository, add the new check and the plugin in the appropriate folder and commit to the repo. This should trigger a jenkins build to generate a new rpm for this package`sensu-team-plugins-vps4.noarch` and install it on the sensu servers.
  - To monitor the progress of the build, you can join the #monitoring-notices slack channel.
  - Sensu checks are located under /etc/sensu/conf.d/team/checks
  - Sensu plugins are located under /etc/sensu/conf.d/team/plugins
  - SSL Certificates are located in /etc/sensu/ssl
  - Client.json and rabbitmq.json are specific to VPS4 and are located in /etc/sensu/conf.d
  - Checks have a document id of `1000694` which should send the SNOW ticket to the DEV-VPS4 group.
  
 ```
 Client.json pasted here for reference, the name and ip address and subscriptions for the server will differ based on the datacenter.
 {
   "client": {
     "name": "p3plvps4rprt01",
     "address": "10.32.65.48",
     "site": "p3",
     "environment": "prod",
     "environment_filter": true,
     "project": "vps4",
     "playbook": "https://confluence.int.godaddy.com/display/HPLAT/vps4app#vps4app-prod",
     "subscriptions": [ "p3plvps4rprt01", "vps4app", "prod_p3", "vps4", "linuxhost" ]
   }
 }
 ```
 
 ```
 rabbitmq.json
 {
   "rabbitmq": {
     "port": 5671,
     "host": "vps4.rmq.sensu.prod.phx3.secureserver.net",
     "user": "sensuvps4",
     "vhost": "/sensu-vps4",
     "password": "6460567d252a8b7e2602f289dd0e91b26ef04c8d",
     "ssl": {
       "private_key_file": "/etc/sensu/ssl/key.pem",
       "cert_chain_file": "/etc/sensu/ssl/cert.pem"
     }
   }
 }
```
 
* Re-start sensu and tail logs
    ```
    service sensu-client start
    tail -50f /var/log/sensu/sensu-client.log
    ```
