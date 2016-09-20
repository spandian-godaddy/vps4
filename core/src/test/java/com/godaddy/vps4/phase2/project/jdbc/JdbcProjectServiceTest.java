package com.godaddy.vps4.phase2.project.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.google.inject.Guice;
import com.google.inject.Injector;

//These tests are broken. Remove the ignore once they have been fixed.
@Ignore 
public class JdbcProjectServiceTest {

	Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource;

    @Before
    @After
    public void truncate() throws SQLException {
        if (dataSource == null) {
            dataSource = injector.getInstance(DataSource.class);
        }
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("TRUNCATE TABLE user_project_privilege CASCADE;");
                statement.executeUpdate("TRUNCATE TABLE project CASCADE;");
                statement.executeUpdate("TRUNCATE TABLE vps4_user CASCADE;");
            }
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("INSERT INTO vps4_user(vps4_user_id, shopper_id) VALUES (1, 'testuser1');");
                statement.executeUpdate("INSERT INTO vps4_user(vps4_user_id, shopper_id) VALUES (2, 'testuser2');");
                Sql.with(dataSource).exec("SELECT create_project(?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), "project4", 1);
                Sql.with(dataSource).exec("SELECT create_project(?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), "project3", 1);
                Sql.with(dataSource).exec("SELECT create_project(?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), "project2", 2);
                Sql.with(dataSource).exec("SELECT create_project(?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), "project1", 2);
            }
        }
    }

    @Test
    public void testGetProjects() {
		ProjectService ps = new JdbcProjectService(dataSource);
        List<Project> projects = ps.getProjects(1, true);
        assertEquals(2, projects.size());
        Project group1 = projects.stream().filter(group -> group.getName().equals("testProject1")).findFirst().get();
        Project group2 = projects.stream().filter(group -> group.getName().equals("testProject2")).findFirst().get();
        assertNotNull(group1);
        assertNotNull(group2);
        projects = ps.getProjects(3, true);
        assertEquals(0, projects.size());
    }

    @Test
    public void testCreateProject() {
        ProjectService projectService = new JdbcProjectService(dataSource);

        String projectName = "testProject";

        Project project = projectService.createProject(projectName, 1);
        assertTrue(project.getProjectId() > 0);
        assertEquals(projectName, project.getName());
        assertEquals("vps4-" + project.getProjectId(), project.getVhfsSgid());
    }
}
