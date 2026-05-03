package com.otpservice.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    private static DatabaseConnection instance;
    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection(Properties props) {
        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseConnection not initialized. Call init() first.");
        }
        return instance;
    }

    public static synchronized void init(Properties props) {
        instance = new DatabaseConnection(props);
        try (Connection c = instance.getConnection()) {
            log.info("Database connection established: {}", props.getProperty("db.url"));
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
