package com.back.simpleDb;

import com.back.Article;
import lombok.SneakyThrows;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @SneakyThrows
    public void run(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            stmt.execute();
        }
    }

    public Sql genSql() {

        return new Sql(this);
    }

    @SneakyThrows
    public long insert(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]); // SQL은 1부터 시작
            }
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    @SneakyThrows
    public int update(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }

    }

    @SneakyThrows
    public int delete(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }

    @SneakyThrows
    public List<Map<String, Object>> selectRows(String sql, Object... params) {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rsmd.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    @SneakyThrows
    public Map<String, Object> selectRow(String sql, Object... params) {
        List<Map<String, Object>> rows = selectRows(sql, params);
        if (rows.isEmpty()) return null;
        return rows.get(0);
    }

    @SneakyThrows
    public <T> List<T> selectRows(String sql, Object[] params, Class<T> type) {
        List<T> rows = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                while (rs.next()) {
                    T instance = type.getDeclaredConstructor().newInstance();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsmd.getColumnLabel(i);
                        Object value = rs.getObject(i);

                        var field = type.getDeclaredField(columnName);
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                    rows.add(instance);
                }
            }
        }

        return rows;
    }

    @SneakyThrows
    public <T> T selectRow(String sql, Object[] params, Class<T> type) {
        List<T> list = selectRows(sql, params, type);
        if (list.isEmpty()) return null;
        return list.get(0);
    }
}
