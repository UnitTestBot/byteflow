package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class SqlInjectionSampleTP {
    boolean isProduction;

    public boolean isAdmin(String userId) throws SQLException {
        String adminUserName;
        if (isProduction) {
            adminUserName = getAdminUserNameProd();
        } else {
            adminUserName = getAdminUserNameDev();
        }

        return checkUserIsAdmin(userId, adminUserName);
    }

    private String getAdminUserNameProd() {
        return "root";
    }

    private String getAdminUserNameDev() {
        return System.getenv("admin_name");
    }

    private boolean checkUserIsAdmin(String userId, String adminName) throws SQLException {
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

}
