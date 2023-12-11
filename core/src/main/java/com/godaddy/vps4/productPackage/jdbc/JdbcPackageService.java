package com.godaddy.vps4.productPackage.jdbc;

import java.util.Collections;
import java.util.Set;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.productPackage.PackageService;
import com.google.inject.Inject;

public class JdbcPackageService implements PackageService {
    private final DataSource dataSource;

    @Inject
    public JdbcPackageService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Set<String> getPackages(Integer... tiers) {
        String q = "SELECT package_id FROM package WHERE tier IN ("
                + String.join(", ", Collections.nCopies(tiers.length, "?"))
                + ")";
        return Sql.with(dataSource)
                  .exec(q, Sql.setOf(rs -> rs.getString("package_id")), (Object[]) tiers);
    }
}
