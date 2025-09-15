package com.back.simpleDb;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;

public class SimpleDb {

    private Connection connection;
    private boolean devMode;


    @SneakyThrows
    public SimpleDb(String host, String username, String password, String dbName) {
        this.connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":3306/" + dbName, username, password
        );
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Connection getConnection() {
        return connection;
    }

    public void run(String sql, Object... params ) {

    }
}
