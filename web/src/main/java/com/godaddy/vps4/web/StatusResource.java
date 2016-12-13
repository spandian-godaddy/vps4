package com.godaddy.vps4.web;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Environment;
import com.godaddy.vps4.jdbc.Sql;

@Vps4Api

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    private static final Logger logger = LoggerFactory.getLogger(StatusResource.class);

    public static class ApplicationStatus {
        public String version = Version.CURRENT;
        public Environment environment = Environment.CURRENT;
        public DatabaseStatus database;
    }

    public static class DatabaseStatus {
        public String version;
    }

    final DataSource dataSource;

    @Inject
    public StatusResource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GET
    public ApplicationStatus getStatus() {
        ApplicationStatus status = new ApplicationStatus();

        DatabaseStatus database = new DatabaseStatus();
        database.version = resolveDatabaseVersion();
        status.database = database;

        return status;
    }

    protected String resolveDatabaseVersion() {
        try {
            return Sql.with(dataSource).exec(
                    "select * from schema_version where success=true order by installed_rank desc limit 1",
                    Sql.nextOrNull(rs -> rs.getString("version")));
        } catch (Exception e) {
            logger.error("Error fetching database version", e);
        }
        return null;
    }

}
