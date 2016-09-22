


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

To rebuild the database without running any tests (from the 'core' project)

    mvn initialize sql:execute@drop-create-database sql:execute@build-database -Prebuild-database

