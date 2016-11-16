package gdg.hfs.useraccount.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import gdg.hfs.Phase2Tests;
import gdg.hfs.mcs.util.jdbc.OrchestrationDataSource;
import gdg.hfs.useraccount.UserAccount;

@Category(Phase2Tests.class)
public class JdbcUserAccountServiceTest {
    private DataSource ds;
    private final Long userId1 = 1L;
    private final Long userId2 = 2L;
    private final String shopperId1 = "mockitoTestShopper1";
    private final String shopperId2 = "mockitoTestShopper2";
    private final UUID accountUuid1 = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private final UUID accountUuid2 = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");
    private Long userAccountId1 = 1L;
    private Long userAccountId2 = 2L;
    private final Instant validOn1 = Instant.now();
    private final Instant validOn2 = Instant.now();
    private final Instant validUntil1 = null;
    private final Instant validUntil2 = Instant.now().plus(Duration.ofHours(1));
    private final List<UserAccount> expectedUserAccountList = new ArrayList<>();

    @Before
    public void before() throws SQLException {
        if (ds == null) {
            ds = OrchestrationDataSource.get();
        }
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            this.truncate(ds);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO mcs_user(user_id, shopper_id) VALUES (" + userId1 + ",'" + shopperId1 + "');");
                stmt.executeUpdate("INSERT INTO mcs_user(user_id, shopper_id) VALUES (" + userId2 + ",'" + shopperId2 + "');");
            }
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT user_account_create(" + userId1 + ",'" + accountUuid1 + "')");
                if (rs.next()) {
                    userAccountId1 = rs.getLong(1);
                }
                assert (userAccountId1 != -1);
            }
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT user_account_create(" + userId2 + ",'" + accountUuid2 + "')");
                if (rs.next()) {
                    userAccountId2 = rs.getLong(1);
                }
                assert (userAccountId2 != -2);
            }
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT user_account_delete(" + userAccountId2 + ")");
                if (rs.next()) {
                    int rowsAffected = rs.getInt(1);
                    assert (rowsAffected != 0);
                }
            }
            conn.commit();
        }

            UserAccount expectedUserAccount1 = new UserAccount(userId1, shopperId1, userAccountId1, accountUuid1, validOn1, validUntil1);
            UserAccount expectedUserAccount2 = new UserAccount(userId2, shopperId2, userAccountId2, accountUuid2, validOn2, validUntil2);
            expectedUserAccountList.add(expectedUserAccount1);
            expectedUserAccountList.add(expectedUserAccount2);
    }

    @Test
    public void testExpectedUserAccountList() {
        if (ds == null) {
            ds = OrchestrationDataSource.get();
        }

        JdbcUserAccountService jdbcUserAccountService = new JdbcUserAccountService(ds);
            /* Get all accounts */
            List<UserAccount> userAccountListAll = jdbcUserAccountService.getUserAccounts(null, null, null, null);
            assertUserAccountLists(expectedUserAccountList, userAccountListAll);
    }

    @Test
    public void testExpectedUserAccountListActive() {
        if (ds == null) {
            ds = OrchestrationDataSource.get();
        }
        JdbcUserAccountService jdbcUserAccountService = new JdbcUserAccountService(ds);

            /* Get all active accounts */
            List<UserAccount> userAccountListActive = jdbcUserAccountService.getActiveUserAccounts(null, null, null, null);
            List<UserAccount> expectedUserAccountListActive = new ArrayList<>();
            expectedUserAccountListActive.add(new UserAccount(userId1, shopperId1, userAccountId1, accountUuid1, validOn1, validOn1));
            assertUserAccountLists(expectedUserAccountListActive, userAccountListActive);
    }

    @Test
    public void testExcludedAccountListAll() {
        if (ds == null) {
            ds = OrchestrationDataSource.get();
        }
        JdbcUserAccountService jdbcUserAccountService = new JdbcUserAccountService(ds);

            /* Ensure if we specify a value from two different user_account records we get neither */
            List<UserAccount> excludedAccountListAll = jdbcUserAccountService.getUserAccounts(userId1, shopperId2, null, null);
            assertTrue(excludedAccountListAll.size() == 0);
    }

    @Test
    public void testExpectedJustUserTwoList() {
        if (ds == null) {
            ds = OrchestrationDataSource.get();
        }
        JdbcUserAccountService jdbcUserAccountService = new JdbcUserAccountService(ds);

            /* Ensure we can pull back a single row */
            List<UserAccount> justUserTwoList = jdbcUserAccountService.getUserAccounts(userId2, shopperId2, userAccountId2, accountUuid2);
            List<UserAccount> expectedJustUserTwoList = new ArrayList<>();
            expectedJustUserTwoList.add(new UserAccount(userId2, shopperId2, userAccountId2, accountUuid2, validOn2, validOn2));
            assertUserAccountLists(expectedJustUserTwoList, justUserTwoList);

    }

    private void truncate(DataSource ds) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("TRUNCATE TABLE user_account CASCADE;");
                stmt.executeUpdate("TRUNCATE TABLE mcs_user CASCADE;");
            }
        }
    }

    private void assertUserAccountLists(List<UserAccount> testUserAccountsList, List<UserAccount> actualUserAccountsList) {
        assertTrue(testUserAccountsList.size() > 0);
        assertTrue(actualUserAccountsList.size() > 0);
        assertEquals(testUserAccountsList.size(), actualUserAccountsList.size());
        for (int i = 0; i < testUserAccountsList.size(); i++) {
            assertEquals(testUserAccountsList.get(i).getAccountUuid(), actualUserAccountsList.get(i).getAccountUuid());
            assertEquals(testUserAccountsList.get(i).getShopperId(), actualUserAccountsList.get(i).getShopperId());
            assertEquals(testUserAccountsList.get(i).getUserAccountId(), actualUserAccountsList.get(i).getUserAccountId());
            assertEquals(testUserAccountsList.get(i).getUserId(), actualUserAccountsList.get(i).getUserId());
            assertNotNull(testUserAccountsList.get(i).getValidOn());
            assertNotNull(actualUserAccountsList.get(i).getValidOn());
            if (testUserAccountsList.get(i).getValidUntil() == null) {
                assertNull(actualUserAccountsList.get(i).getValidUntil());
            } else {
                assertNotNull(actualUserAccountsList.get(i).getValidUntil() != null);
            }
        }
    }
}
