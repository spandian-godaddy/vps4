Getting Started developing on VPS4

- git clone vps4 code locally:
    - https://github.secureserver.net/vps4/vps4
- download Java 8 SDK
- download eclipse Java EE
- create maven settings file:
File: ~/.m2/settings.xml
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

- import code into eclipse
    - open eclipse, select File->Import->Maven->Existing Maven Projects
    - Browse to dir of vps4 git code
    - Select all and finish
    - In project explorer, right click vps4-core->Maven->Update project
    - Click on vps-core/pom.xml in project explorer, go to menu Run->Run As->maven install

- Create Eclipse Run Configurations
    - Run->Run configurations
    - Java Application, create New
    - Orchestration Engine
        - Name: Local Orchestration Engine
        - Project: Browse to project: vps4-orchestration-plugin
        - Main class: Search for OrchestrationEngineServer
        - Check box Include inherited mains
        - Arguments tab: Add VM arguments: -Dorchestration.engine.mode=memory
        - Apply
    - Web Server
        - Name: WebServer
        - Project: Browse to project: vps4-web
        - Main class: Search for Vps4Application
        - Apply

- Setup Postgres database
    - Install postgresql locally or install vagrant
        - Install homebrew
        - brew install postgresql
        - createuser -s -r postgres
        - Test using cmd: psql -U postgres postgres
        - Modify /etc/hosts to redirect domain vps4-local-dbserver.dev-godaddy.com
        - Locally if postgres installed locally or at vagrant IP if using vagrant
            - echo "127.0.0.1   vps4-local-dbserver.dev-godaddy.com" >> /etc/hosts
    - Via commandline, in vps4/core dir
mvn initialize sql:execute@drop-create-database -Prebuild-database
mvn initialize flyway:migrate

- Creating first VM
    - Run Orchestration Engine
    - Run WebServer
    - Use a tool to generate a uuid like python: python -c "from uuid import uuid4; print(uuid4())"
        - Ex:17f0a467-293b-45ed-9990-a75d3d3adaa5
    - Create a vps4 credit
    - Swagger URL for vps4 local: http://localhost:8089/swagger/#!/vms/provisionVm
    - 
    - Body of POST:
{
  "name": "sejvm01",
  "orionGuid": "1637bf9c-18aa-11e7-83d9-60f81db99564",
  "image": "centos-7",
  "dataCenterId": 1,
  "username": "sjohnson",
  "password": "onevps4ME!"
}
 
- Import HFS web developer client cert into browser (Chrome?) to use hfs swagger UI
    - Repeat below steps for any environment needed.  Steps are for STAGE
    - Generate the pksc12 cert
cd workspaces/vps4
git clone https://github.secureserver.net/hfs/Creds.git
mkdir -p hfs_swagger_certs/{DEV,TEST,STAGE,PROD}
cd Creds/ssl_ca/hfs/STAGE/
cp hfs_end_web_developer.crt hfs_end_web_developer.key hfs_int_web.crt hfs_root.crt \
        ~workspaces/vps4/hfs_swagger_certs/STAGE
cd !$
cat hfs_int_web.crt hfs_root.crt >> temp_certfile
openssl pkcs12 -export -passout pass:changeit -in hfs_end_web_developer.crt -inkey hfs_end_web_developer.key -certfile temp_certfile -out hfs_end_web_developer.p12

    - Import into Mac KeyChain
        - Open KeyChain Access
        - Click login and My Certificates
        - File->Import Items, browse to new .p12 file
        - Insert password from above: changeit

Use HFS Mock:
 - Add to VM arguments in orchestration engine run configurations: -Dvps4.hfs.mock=true
