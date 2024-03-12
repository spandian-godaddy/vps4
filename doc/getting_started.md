# Getting Started developing on VPS4

## Access Rights

- There are several requests for access you need to make to get started on developing:
    - request to be added to the `Jomax/Dev-VPS4` AD group (for the VPS4 API)
        - https://godaddy.service-now.com/gdsp
        - Employee Technology > Access > Security Group Updates > Update My Security Groups
    - request to be added to the `DC1/admins_vps4` AD group (for access to infrastructure servers)
        - https://godaddy.service-now.com/gdsp
        - Employee Technology > Access > Security Group Updates > Update My Security Groups
    - request CRM - Validation Skip Access (for viewing customer dashboards)
        - https://godaddy.service-now.com/gdsp
        - search for "skip access" and choose `CRM - Validation Skip Access`
    - Note: the requests may be automatically marked as completed, but it usually takes a few hours for the system to update and for you to gain access.

## Getting Started

- clone the VPS4 code locally
    - set up SSH keys for your account
    - `git clone git@github.com:gdcorp-partners/vps4.git`
    - if you need write access to this repo, ask a VPS4 team member to add you as a collaborator in GitHub
- [install and configure maven](#maven-configuration)
- [get configs from AWS secrets](#get-configs-from-aws-secrets)
- [set up your local database](#database-configuration)
- install [tartufo](https://tartufo.readthedocs.io/en/stable/installation.html)
- install [pre-commit](https://pre-commit.com/#install) and follow the quick start
    - the `.pre-commit-config.yaml` file is already set up and included in the VPS4 repo
- install the [Java 8 SDK](https://www.oracle.com/java/technologies/downloads/#java8)
- install and configure your preferred IDE
    - [IntelliJ](#intellij-setup)
    - [Eclipse](#eclipse-setup)

## Maven Configuration

- install Maven
    - for Mac with homebrew, use `brew install maven`
- check that Maven is using the correct version of Java with `mvn -v`
    - if the  Java version larger than 1.8, make a ``~/.mavenrc`` file with this content:
        ```
        JAVA_HOME=`/usr/libexec/java_home -v 1.8.0`
        ```
- create a settings file
    - `~/.m2/settings.xml`
        ```xml
        <settings>
            <profiles>
                <profile>
                    <id>default</id>
                    <repositories>
                        <repository>
                            <id>default</id>
                            <name>hosting local repo</name>
                            <url>https://gdartifactory1.jfrog.io/artifactory/java-hostingcore-local/</url>
                            <layout>default</layout>
                        </repository>
                        <repository>
                            <id>java-sso</id>
                            <name>java sso local repo</name>
                            <url>https://gdartifactory1.jfrog.io/artifactory/java-sso-local/</url>
                            <layout>default</layout>
                        </repository>
                    </repositories>
                </profile>
            </profiles>
            <activeProfiles>
                <activeProfile>default</activeProfile>
            </activeProfiles>
            <servers>
                <server>
                    <id>default</id>
                    <username>ci_hostingcore</username>
                    <password>CHANGE-THIS</password>
                </server>
                <server>
                    <id>java-sso</id>
                    <username>ci_hostingcore</username>
                    <password>CHANGE-THIS</password>
                </server>
            </servers>
        </settings>
        ```
    - you can get the real password from a teammate or the CICD server

## Get Configs from AWS Secrets

- we have [a Jenkins job](https://vps4.jenkins.int.godaddy.com/view/AWS%20Secrets/job/AWS%20Get%20Secret/) for retrieving secrets from AWS
- for each secret in the table, run the Jenkins job and store the result from the Jenkins workspace on your laptop

| Secret Name                    | File Name               | Environment | Path on Local Machine                                                         |
|--------------------------------|-------------------------|-------------|-------------------------------------------------------------------------------|
| /base/config.properties        | config.properties       | dev         | core/src/main/resources/com/godaddy/vps4/config/base/config.properties        |
| /local/config.properties       | config.properties       | dev         | core/src/main/resources/com/godaddy/vps4/config/local/config.properties       |
| /base/hfs.api.crt              | hfs.api.crt             | dev         | core/src/main/resources/com/godaddy/vps4/config/base/hfs.api.crt              |
| /base/hfs.api.key              | hfs.api.key             | dev         | core/src/main/resources/com/godaddy/vps4/config/base/hfs.api.key              |
| /local/messaging.api.crt       | messaging.api.crt       | dev         | core/src/main/resources/com/godaddy/vps4/config/local/messaging.api.crt       |
| /local/messaging.api.key       | messaging.api.key       | dev         | core/src/main/resources/com/godaddy/vps4/config/local/messaging.api.key       |
| /local/entitlements.api.crt    | entitlements.api.crt    | dev         | core/src/main/resources/com/godaddy/vps4/config/local/entitlements.api.crt    |
| /local/entitlements.api.key    | entitlements.api.key    | dev         | core/src/main/resources/com/godaddy/vps4/config/local/entitlements.api.key    |
| /local/firewall.api.crt        | firewall.api.crt        | dev         | core/src/main/resources/com/godaddy/vps4/config/local/firewall.api.crt        |
| /local/firewall.api.key        | firewall.api.key        | dev         | core/src/main/resources/com/godaddy/vps4/config/local/firewall.api.key        |
| /local/vps4.shopper.crt        | vps4.shopper.crt        | dev         | core/src/main/resources/com/godaddy/vps4/config/local/vps4.shopper.crt        |
| /local/vps4.shopper.key        | vps4.shopper.key        | dev         | core/src/main/resources/com/godaddy/vps4/config/local/vps4.shopper.key        |
| /local/vps4.api.crt            | vps4.api.crt            | dev         | core/src/main/resources/com/godaddy/vps4/config/local/vps4.api.crt            |
| /local/vps4.api.key            | vps4.api.key            | dev         | core/src/main/resources/com/godaddy/vps4/config/local/vps4.api.key            |
| /local/password_encryption.key | password_encryption.key | dev         | core/src/main/resources/com/godaddy/vps4/config/local/password_encryption.key |

## Database Configuration

- install postgresql locally or use vagrant
    - we use v9.6.8, but brew doesn't support that version anymore, so 10 will work
    - for Mac with homebrew:
        ```
        brew install postgresql@10
        brew services start postgresql
        createuser -s -r postgres
        ```
    - add the following line to your `/etc/hosts` file, substituting 127.0.0.1 with your vagrant IP if using vagrant:
        ```
        127.0.0.1 vps4-local-dbserver.dev-godaddy.com
        ```
- in `vps4/core` directory, initialize postgres db:
    ```bash
    mvn initialize sql:execute@drop-create-database -Prebuild-database
    mvn initialize flyway:migrate
    ```
- any future migrations can be applied with `mvn initialize flyway:migrate` in `vps4/core`
- if you happen to get an error regarding `vhfs-sysadmin-common-lib` contact a team member to help you get a copy of the version we use from artifactory since HFS deleted it.

## IntelliJ Setup

- install IntelliJ
    - the community edition is all you really need
    - if you prefer the ultimate edition, you'll need a license
        - submit a request (example: [RITM0120501](https://godaddy.service-now.com/nav_to.do?uri=sc_req_item.do?sys_id=b2e5047c37708384ce4fb15ec3990e9a))
        - wait for the request to be approved. This may take a few days
        - the JetBrains license will be connected to your GoDaddy email, and you will then be able to activate the license for IntelliJ or any other JetBrains IDEs
- import code into Intellij Idea
    - open the project with `Projects > Open > ~/path/to/vps4`
    - set the SDK to Java 8 in `Module Settings > Project > SDK`
- create Intellij Run Configurations
    - Click on "Add Configuration" on the top right bar
    - Click `Add new... > Application`
    - Create configurations using the [IntelliJ Run Configs](../doc/intellij_run_configs.md) doc
        - In the "Modify options" dropdown, make sure "Add VM options" is checked
        - The majority of development can be done with just the `Local Orchestration Engine (Non-plugin)` and `Vps4 API` configs
- Settings Sharing
    - Install the Repository Settings plugin at https://plugins.jetbrains.com/plugin/7566-settings-repository
    - Go to File -> Manage IDE Settings -> Settings Repository...
    - Put in the URL of the settings repo: https://github.com/gdcorp-partners/vps4-intellijsettings.git
    - Choose the appropriate setting, Merge, Overwrite Local, Overwrite Remote. Overwrite local is recommended to begin to not overwrite the teams settings.


## Eclipse Setup

- install the Eclipse IDE for Java Developers
- import code into Eclipse
    - open Eclipse, select File -> Import -> Maven -> Existing Maven Projects
    - browse to dir of vps4 git code
    - Select all and finish
    - click on `vps4-core/pom.xml` in project explorer, go to menu Run -> Run As -> maven install
    - in project explorer, right click vps4-core -> Maven -> Update project
    - after updating project, there may still be three errors in the "Problems" tab of Eclipse
        - if the errors say "plugin execution not covered by lifecycle configuration", this can be fixed by right clicking each error and ignoring in Eclipse
- create Eclipse Run Configurations
    - Run -> Run configurations
    - Java Application, create New launch configurations
        - Orchestration Engine (**Refer to the [readme in the vps4-orchestration-runner](../vps4-orchestration-runner/README.md) for instructions on setting up the run config for orch engine**)
            - <s> Name: LocalOrchestrationEngine 
            - Project: Browse to project: vps4-orchestration-plugin
            - Main class: Search for OrchestrationWebApplication
            - Check box "Include inherited mains..."
            - Arguments tab: Add VM arguments: ```-Dorchestration.engine.mode=memory -Dvps4.config.mode=file -Dhfs.http.port=8088```
            - Apply
            </s>
        - Web Server
            - Name: WebServer
            - Project: Browse to project: vps4-web
            - Main class: Search for Vps4Application
            - Arguments tab: Add VM arguments: ```-Dvps4.user.fake=false -Dvps4.config.mode=file -Dorchestration.engine.clustered=false```
            - Apply

## Creating Your First VM

- run your local orchestration engine
- run your local vps4 web server
- set your `authorization` request header
    - header should be in the format `Authorization: sso-jwt YOUR_DEV_IDP_TOKEN`
    - the token can be found in your `auth_idp` cookie after signing in to [dev](https://sso.dev-godaddy.com/), or using a tool like [gotjwt](https://github.secureserver.net/sjohnson/gotjwt)
- create a credit with the [POST /api/credits](http://localhost:8089/swagger/#!/credits/createCredit) endpoint
    - Example:
        ```json
        {
            "controlPanel": "myh",
            "managedLevel": 0,
            "monitoring": 0,
            "operatingSystem": "linux",
            "resellerId": 1,
            "shopperId": "YOUR_SHOPPER_ID",
            "tier": 10
        }
        ```
- create a VM with the [POST /api/vms](http://localhost:8089/swagger/#!/vms/provisionVm) endpoint
    - Example:
        ```json
        {
          "name": "vm01",
          "orionGuid": "sameAsAccountGuidAbove",
          "image": "hfs-debian10",
          "dataCenterId": 1,
          "username": "myusername",
          "password": "onevps4ME!"
        }
        ```
    - available image names can be found in vps4 database 'image' table

## Calling the HFS API Directly

- if you want to call the HFS API directly, you'll need the HFS web developer client certs in your system keychain
    - ask HFS team for read collaborator access to the [hfs/Creds](https://github.secureserver.net/hfs/Creds) repo, and then git clone it locally
    - for each environment, find its pkcs12 cert (example: `Creds/ssl_ca/hfs/STAGE/hfs_end_web_developer.p12`)
    - import the .p12 file into Mac KeyChain
        - open KeyChain Access
        - File -> Import Items, browse to .p12 file
        - insert password `changeit`
