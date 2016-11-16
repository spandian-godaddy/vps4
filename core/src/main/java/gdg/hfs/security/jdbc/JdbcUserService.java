package gdg.hfs.security.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.security.Vps4User;

import gdg.hfs.security.Vps4UserService;

public class JdbcUserService implements Vps4UserService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcUserService.class);

    private final DataSource dataSource;

    @Inject
    public JdbcUserService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Vps4User getUserForShopper(String shopperId) {
        return Sql.with(dataSource).exec("SELECT user_id, shopper_id, name FROM mcs_user WHERE shopper_id=?",
                Sql.nextOrNull(this::mapUser),
                shopperId);
    }

    @Override
    public Vps4User getUserForId(long userId) {
        return Sql.with(dataSource).exec("SELECT user_id, shopper_id, name FROM mcs_user WHERE user_id=?",
                Sql.nextOrNull(this::mapUser),
                userId);
    }

    @Override
    public Vps4User getOrCreateUserForShopper(String shopperId, String username) {

        // if the user doesn't exist yet, create it
        Sql.with(dataSource).exec("INSERT INTO mcs_user (shopper_id, username) "
                + " SELECT ?, ? WHERE NOT EXISTS ( SELECT 1 FROM mcs_user WHERE shopper_id=? )",
                null,
                shopperId, username, shopperId);

        Vps4User user = getUserForShopper(shopperId);
        if (user == null) {
            throw new IllegalStateException("Unable to lazily create user for shopper " + shopperId);
        }

        logger.debug("shopperId {} => userId {}", shopperId, user.getId());
        return user;
    }

    protected Vps4User mapUser(ResultSet rs) throws SQLException {

        long userId = rs.getLong("user_id");
        String shopperId = rs.getString("shopper_id");
        String name = rs.getString("name");

        return new Vps4User(name, userId, shopperId);
    }

}
