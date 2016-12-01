


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


Encrypted Configuration
=======================

Configuration files are kept in core/src/main/resources/{environment}/

`vps4.properties` contains all the properties for that particular environment

Secrets like, for instance, production database credentials, are stored in an encrypted
properties file in the environment directory:

`vps4.enc.properties` is a properties file encrypted with the environment's public key

The public and private keys for the respective environments are read from the classpath at:

    vps4.{environment}.priv.pem
    vps4.{environment}.pub.pem

To modify encrypted properties, unencrypt the `vps.enc.properties` file for a particular environment: 

    mvn exec:java@decrypt-config -Dvps4.env={environment}

This will use the environment private key to decrypt `vps4.enc.properties` into `vps4.unenc.properties`.

Modify `vps4.unenc.properties`, then re-encrypt:

    mvn exec:java@encrypt-config -Dvps4.env={environment}


_Never_ check in the `vps4.unenc.properties` files, as these contain the plaintext secrets.
These files are explicitly ignored in .gitignore.
