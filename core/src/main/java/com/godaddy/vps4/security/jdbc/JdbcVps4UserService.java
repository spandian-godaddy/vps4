package com.godaddy.vps4.security.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class JdbcVps4UserService implements Vps4UserService {

    private static final String SQL_UNIQUE_CODE = "23505";

    private static final Logger logger = LoggerFactory.getLogger(JdbcVps4UserService.class);

    private final DataSource dataSource;

    @Inject
    public JdbcVps4UserService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Vps4User getUser(String shopperId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id, shopper_id, customer_id FROM vps4_user WHERE shopper_id=?",
                Sql.nextOrNull(this::mapUser),
                shopperId);
    }

    @Override
    public Vps4User getUser(long userId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id, shopper_id, customer_id FROM vps4_user WHERE vps4_user_id=?",
                Sql.nextOrNull(this::mapUser),
                userId);
    }

    @Override
    public Vps4User getUser(UUID customerId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id, shopper_id, customer_id FROM vps4_user WHERE customer_id=?",
                Sql.nextOrNull(this::mapUser),
                customerId);
    }

    @Override
    public Vps4User getOrCreateUserForShopper(String shopperId, String resellerId, UUID customerId) {
        Vps4User user = getUser(shopperId);
        if(user == null) {
            try {
                user = Sql.with(dataSource).exec(
                        "INSERT INTO vps4_user (shopper_id, reseller_id, customer_id) VALUES (?, ?, ?) RETURNING *",
                        Sql.nextOrNull(this::mapUser),
                        shopperId, resellerId, customerId);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof SQLException) {
                    SQLException cause = (SQLException) e.getCause();
                    if (cause.getSQLState().equals(SQL_UNIQUE_CODE)) {
                        user = getUser(shopperId);
                        if (user == null)
                            throw new RuntimeException(String.format("Unable to find or create user for shopper %s", shopperId), e);
                    }
                    else {
                        throw e;
                    }
                }
                else {
                    throw e;
                }
            }
        }

        logger.debug("shopperId {} => userId {}", shopperId, user.getId());
        return user;
    }

    protected Vps4User mapUser(ResultSet rs) throws SQLException {

        long userId = rs.getLong("vps4_user_id");
        String shopperId = rs.getString("shopper_id");
        UUID customerId = UUID.fromString(rs.getString("customer_id"));

        return new Vps4User(userId, shopperId, customerId);
    }

}
