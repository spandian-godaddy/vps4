

Versioning
==========

Maven uses a standard for versioning:  http://www.mojohaus.org/versions-maven-plugin/version-rules.html

    <MajorVersion [> . <MinorVersion [> . <IncrementalVersion ] ] [> - <BuildNumber | Qualifier ]>

Major.Minor.Incremental-JenkinsBuildNumber-GitCommitFirst7

mvn versions:set -DgroupId=com.godaddy.vps4  \
                 -DartifactId=vps4-parent    \
                 -DnewVersion=0.0.1-SNAPSHOT \
                 -DgenerateBackupPoms=false
