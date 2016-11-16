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

public class JdbcVps4UserService implements Vps4UserService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcVps4UserService.class);

    private final DataSource dataSource;

    @Inject
    public JdbcVps4UserService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Vps4User getUserForShopper(String shopperId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id, shopper_id FROM vps4_user WHERE shopper_id=?",
                Sql.nextOrNull(this::mapUser),
                shopperId);
    }

    @Override
    public Vps4User getUserForId(long userId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id, shopper_id FROM vps4_user WHERE vps4_user_id=?",
                Sql.nextOrNull(this::mapUser),
                userId);
    }

    @Override
    public Vps4User getOrCreateUserForShopper(String shopperId) {

        // if the user doesn't exist yet, create it
        Sql.with(dataSource).exec("INSERT INTO vps4_user (shopper_id) "
                + " SELECT ? WHERE NOT EXISTS ( SELECT 1 FROM vps4_user WHERE shopper_id=? )",
                null,
                shopperId, shopperId);

        Vps4User user = getUserForShopper(shopperId);
        if (user == null) {
            throw new IllegalStateException("Unable to lazily create user for shopper " + shopperId);
        }

        logger.debug("shopperId {} => userId {}", shopperId, user.getId());
        return user;
    }

    protected Vps4User mapUser(ResultSet rs) throws SQLException {

        long userId = rs.getLong("vps4_user_id");
        String shopperId = rs.getString("shopper_id");

        return new Vps4User(userId, shopperId);
    }

}
