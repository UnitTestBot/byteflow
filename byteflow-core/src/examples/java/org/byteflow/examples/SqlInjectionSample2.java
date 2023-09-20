package org.byteflow.examples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class SqlInjectionSample2 {
    boolean isProduction;

    public boolean isAdmin(String userId) throws SQLException {
        String adminUserName;
        if (isProduction) {
            adminUserName = getAdminUserNameProd();
        } else {
            adminUserName = getAdminUserNameDev();
        }

        boolean isAdmin;
        if (isProduction) {
            isAdmin = checkUserIsAdminProd(userId, adminUserName);
        } else {
            isAdmin = checkUserIsAdminProd(userId, adminUserName);
        }

        return isAdmin;
    }

    private String getAdminUserNameProd() {
        return "root";
    }

    private String getAdminUserNameDev() {
        return System.getenv("admin_name");
    }

    private boolean checkUserIsAdminProd(String userId, String adminName) throws SQLException {
        String adminId;
        try (Connection dbConnection = DriverManager.getConnection("url://127.0.0.1:8080");
             Statement statement = dbConnection.createStatement()) {
            // SECS: potential SQL injection
            ResultSet rs = statement.executeQuery("SELECT id from users where name='" + adminName + "'");
            if (rs.next()) {
                adminId = rs.getString(0);
            } else {
                throw new IllegalStateException("No admin id");
            }
        }

        if (adminId == null) {
            throw new IllegalStateException("No admin id");
        }

        return adminId.equals(userId);
    }

    private boolean checkUserIsAdminDev(String userId, String adminName) {
        String adminId;
        if ("root_1".equals(adminName)) {
            adminId = "1";
        } else if ("root_2".equals(adminName)) {
            adminId = "2";
        } else {
            adminId = "0";
        }

        return adminId.equals(userId);
    }

}
