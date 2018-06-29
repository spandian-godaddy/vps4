# Getting Started developing on VPS4


- request to be added to the `Dev-VPS4` AD group
    - https://godaddy.service-now.com/gdsp
    - Service Catalog -> My Digital Experience -> Access -> Security Group Updates -> Update My Security Groups
- git clone vps4 code locally:
    - https://github.secureserver.net/vps4/vps4
    - note: you will not have write access to this repo until someone adds you as a collaborator or to the vps4 team in GitHub
- download Java 8 SDK
- download Eclipse IDE for Java Developers
    - you can choose to use another IDE, like IntelliJ IDEA (Community Edition should be sufficient & does not require a license), but these specific instructions are catered for Eclipse setup
- create maven settings file (be sure to get the password for line 22):
> File: ~/.m2/settings.xml
```xml
<settings>
    <profiles>
        <profile>
            <id>default</id>
            <repositories>
                <repository>
                    <id>default</id>
                    <name>hosting local repo</name>
                    <url>https://artifactory.secureserver.net/artifactory/java-hostingcore-local/</url>
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
            <password>ask-somone-on-the-team-for-real-password</password>
        </server>
    </servers>
</settings>
```

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
        - Orchestration Engine
            - Name: LocalOrchestrationEngine
            - Project: Browse to project: vps4-orchestration-plugin
            - Main class: Search for OrchestrationWebApplication
            - Check box "Include inherited mains..."
            - Arguments tab: Add VM arguments: ```-Dorchestration.engine.mode=memory -Dvps4.config.mode=file -Dhfs.http.port=8088```
            - Apply
        - Web Server
            - Name: WebServer
            - Project: Browse to project: vps4-web
            - Main class: Search for Vps4Application
            - Arguments tab: Add VM arguments: ```-Dvps4.user.fake=false -Dvps4.config.mode=file -Dorchestration.engine.clustered=false```
            - Apply

- replace Java limited encryption jars if you’ve never done so
    - Download the unlimited jce jars and see the readme.txt for instructions: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html

- in `vps4/core` run migrations as necessary / after pulling any updates
    - `mvn initialize flyway:migrate`

- setup Postgres database
    - install postgresql locally or install vagrant
    - for Mac, one way to do it:
        - install homebrew
        - `brew install postgresql`
		- `brew services start postgresql`
        - `createuser -s -r postgres`
        - test using cmd: `psql -U postgres postgres`
        - modify `/etc/hosts` to redirect domain vps4-local-dbserver.dev-godaddy.com
        - locally if postgres installed locally or at vagrant IP if using vagrant
            - ```echo "127.0.0.1   vps4-local-dbserver.dev-godaddy.com" >> /etc/hosts```
    - via commandline, in `vps4/core` dir, initialize postgres db:
        ```bash
        mvn initialize sql:execute@drop-create-database -Prebuild-database
        mvn initialize flyway:migrate
        ```

- import HFS web developer client cert into browser (Chrome?) to use HFS swagger UI
    - ask HFS team for read collaborator access to the `hfs/Creds` repo, and then git clone it locally
        - https://github.secureserver.net/hfs/Creds
    - repeat below steps for any environment needed.  Steps are for `STAGE`

    - EITHER use the existing pksc12 cert:
        - `Creds/ssl_ca/hfs/STAGE/hfs_end_web_developer.p12`
    - OR generate the pksc12 cert if needed:
        - Example:
        ```bash
        mkdir -p hfs_swagger_certs/{DEV,TEST,STAGE,PROD}
        cd Creds/ssl_ca/hfs/STAGE/
        cp hfs_end_web_developer.crt hfs_end_web_developer.key hfs_int_web.crt hfs_root.crt \
                ~workspaces/vps4/hfs_swagger_certs/STAGE
        cd !$
        cat hfs_int_web.crt hfs_root.crt >> temp_certfile
        openssl pkcs12 -export -passout pass:changeit -in hfs_end_web_developer.crt -inkey hfs_end_web_developer.key -certfile temp_certfile -out hfs_end_web_developer.p12
        ```
  - THEN Import into Mac KeyChain
      - open KeyChain Access
      - click login and My Certificates
      - File -> Import Items, browse to .p12 file
      - insert password from above: `changeit`
      - if desired, Always Trust this certificate (right click certificate in Keychain Access, Trust -> When using this certificate Always Trust; exiting modal will prompt for password to Update Settings)

- FYI, if you wanted to bypass HFS altogether, use the Mock HFS:
  - add to VM arguments in `LocalOrchestrationEngine` run configuration: `-Dvps4.hfs.mock=true`

- create your first VM
    - Run `LocalOrchestrationEngine`
    - Run `WebServer`
    - use a tool to generate a uuid, ex. python: `python -c "from uuid import uuid4; print(uuid4())"`
        - ex: `17f0a467-293b-45ed-9990-a75d3d3adaa5`
    - create a vps4 credit via the HFS swagger UI: https://hfs.api.stg-godaddy.com/#!/ecomm/createAccount
        - Example POST:
        ```json
        {
          "shopper_id": "yourDevShopperId",
          "account_guid": "uuidGeneratedAbove",
          "product": "vps4",
          "status": "active",
          "plan_features": {
            "control_panel_type": "cPanel",
            "managed_level": "2",
            "monitoring": "0",
            "operatingsystem": "Linux",
            "tier": "20"
          },
          "product_meta": {},
          "hfs_meta": {},
          "sub_account_shopper_id": null,
          "reseller_id": "1"
        }
        ```
    - create a VM using this credit via the VPS4 local swagger UI: http://localhost:8089/swagger/#!/vms/provisionVm
        - you have to use an appropriate request header when making calls or you will receive a “missing auth / no SSO token found in your request” error
            - EITHER log in to https://www.dev-godaddy.com/ and get your `auth_idp` cookie value (can use Chrome plugin like EditThisCookie)
            - OR use something like https://github.secureserver.net/sjohnson/gotjwt
            - THEN add a `Authorization` `sso-jwt appropriateValue` request header (can use Chrome plugin like Modify Headers)
        - Example POST:
        ```json
        {
          "name": "vm01",
          "orionGuid": "product_id_from_credit",
          "image": "hfs-centos-7-cpanel-11",
          "dataCenterId": 1,
          "username": "myusername",
          "password": "onevps4ME!"
        }
        ```
        - note: `Error while creating a backup schedule for VM: someGUID. Error details: {} java.net.ConnectException: Connection refused; Connect to 127.0.0.1:8180` during creates is normal / expected if you do not have a real scheduler
