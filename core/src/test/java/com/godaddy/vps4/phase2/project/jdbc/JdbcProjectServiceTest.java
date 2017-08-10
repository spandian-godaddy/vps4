package com.godaddy.vps4.phase2.project.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcProjectServiceTest {

	Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource;

    Map<String, Vps4User> users = new HashMap<String, Vps4User>();
    Map<String, Project> projects = new HashMap<String, Project>();

    @Before
    public void setupTests() throws SQLException {

        if (dataSource == null) {
            dataSource = injector.getInstance(DataSource.class);
        }
        ProjectService ps = new JdbcProjectService(dataSource);
        Vps4UserService vps4UserService = new JdbcVps4UserService(dataSource);



        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                users.put("user1", vps4UserService.getOrCreateUserForShopper("testuser1"));
                users.put("user2", vps4UserService.getOrCreateUserForShopper("testuser2"));
                projects.put("project4", ps.createProject("project4", users.get("user1").getId(), "unit-test"));
                projects.put("project3", ps.createProject("project3", users.get("user1").getId(), "unit-test"));
                projects.put("project2", ps.createProject("project2", users.get("user2").getId(), "unit-test"));
                projects.put("project1", ps.createProject("project1", users.get("user2").getId(), "unit-test"));

            }
        }
    }

    @After
    public void teardownTests() throws SQLException{
        if (dataSource == null) {
            dataSource = injector.getInstance(DataSource.class);
        }
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                for(Vps4User user: users.values()){
                    statement.executeUpdate("DELETE FROM user_project_privilege where vps4_user_id = " + user.getId() + ";");
                    statement.executeUpdate("DELETE FROM vps4_user where vps4_user_id = " + user.getId() + ";");
                }
                statement.executeUpdate("DELETE FROM project where vhfs_sgid like 'unit-test%';");
            }
        }
    }

    @Test
    public void testGetProjects() {
		ProjectService ps = new JdbcProjectService(dataSource);
        List<Project> projects = ps.getProjects(users.get("user1").getId(), true);
        assertEquals(2, projects.size());
        Project group1 = projects.stream().filter(group -> group.getName().equals("project4")).findFirst().get();
        Project group2 = projects.stream().filter(group -> group.getName().equals("project3")).findFirst().get();
        assertNotNull(group1);
        assertNotNull(group2);
        projects = ps.getProjects(users.get("user2").getId() + 1, true);
        assertEquals(0, projects.size());
    }

    @Test
    public void testCreateProject() {
        ProjectService projectService = new JdbcProjectService(dataSource);

        String projectName = "testProject";

        Project project = projectService.createProject(projectName, users.get("user1").getId(), "vps4-test-");
        assertTrue(project.getProjectId() > 0);
        assertEquals(projectName, project.getName());
        assertEquals("vps4-test-" + project.getProjectId(), project.getVhfsSgid());
    }
}
