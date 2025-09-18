package com.back.simpleDb;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder;
    private final List<Object> parameters;

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
        this.sqlBuilder = new StringBuilder();
        this.parameters = new ArrayList<>();
    }

    public Sql append(String sql) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");
        sqlBuilder.append(sql);
        return this;
    }

    public Sql append(String sql, Object... parameter) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");
        sqlBuilder.append(sql);
        for (Object param : parameter) {
            parameters.add(param);
        }
        return this;
    }

    public Sql appendIn(String sql, Object... values) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");

        if (sql.contains("VALUES") && sql.endsWith("?)")) {
            String additionalPlaceholders = String.join(", ", java.util.Collections.nCopies(values.length - 1, "?"));
            String modifiedSql = sql.replace("?)", "?, " + additionalPlaceholders + ")");
            sqlBuilder.append(modifiedSql);
        } else {
            String placeholders = String.join(", ", java.util.Collections.nCopies(values.length, "?"));
            sqlBuilder.append(sql.replace("?", placeholders));
        }

        java.util.Collections.addAll(parameters, values);
        return this;
    }

    public long insert() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {
                bindParametersAndLog(pstmt);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    return rs.next() ? rs.getLong(1) : -1;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                bindParametersAndLog(pstmt);
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                bindParametersAndLog(pstmt);
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        return executeSelect(rs -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = rs.getObject(i);
                    row.put(meta.getColumnName(i),
                            value instanceof Timestamp ? ((Timestamp) value).toLocalDateTime() :
                                    value instanceof byte[] && ((byte[]) value).length == 1 ? ((byte[]) value)[0] != 0 : value);
                }
                rows.add(row);
            }
            return rows;
        });
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public LocalDateTime selectDatetime() {
        return executeSelect(rs -> rs.next() ? rs.getTimestamp(1).toLocalDateTime() : null);
    }

    public Long selectLong() {
        return executeSelect(rs -> rs.next() ? rs.getLong(1) : null);
    }

    public String selectString() {
        return executeSelect(rs -> rs.next() ? rs.getString(1) : null);
    }

    public Boolean selectBoolean() {
        return executeSelect(rs -> {
            if (!rs.next()) return null;
            Object value = rs.getObject(1);
            if (value instanceof byte[] && ((byte[]) value).length == 1) {
                return ((byte[]) value)[0] != 0;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            return null;
        });
    }

    public List<Long> selectLongs() {
        return executeSelect(rs -> {
            List<Long> longs = new ArrayList<>();
            while (rs.next()) {
                longs.add(rs.getLong(1));
            }
            return longs;
        });
    }

    private <T> T executeSelect(ResultProcessor<T> processor) {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                bindParametersAndLog(pstmt);

                try (ResultSet rs = pstmt.executeQuery()) {
                    return processor.process(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ResultProcessor<T> {
        T process(ResultSet rs) throws SQLException;
    }

    private void bindParametersAndLog(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            pstmt.setObject(i + 1, parameters.get(i));
        }

        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==\n" + buildRawSql());
        }
    }

    private String buildRawSql() {
        String result = sqlBuilder.toString();
        for (Object param : parameters) {
            if (param instanceof String) {
                result = result.replaceFirst("\\?", "'" + param + "'");
            } else {
                result = result.replaceFirst("\\?", String.valueOf(param));
            }
        }
        return result;
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return executeSelect(rs -> {
            List<T> objects = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();

            while (rs.next()) {
                try {
                    T obj = clazz.getDeclaredConstructor().newInstance();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = rs.getObject(i);

                        if (value instanceof Timestamp) value = ((Timestamp) value).toLocalDateTime();
                        if (value instanceof byte[] && ((byte[]) value).length == 1) value = ((byte[]) value)[0] != 0;

                        String setter = "set" + meta.getColumnName(i).substring(0, 1).toUpperCase() + meta.getColumnName(i).substring(1);
                        if (setter.equals("setIsBlind")) setter = "setBlind";

                        if (value != null) {
                            try {
                                obj.getClass().getMethod(setter, value.getClass()).invoke(obj, value);
                            } catch (Exception ignored) {}
                        }
                    }
                    objects.add(obj);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create instance of " + clazz.getSimpleName(), e);
                }
            }
            return objects;
        });
    }

    public <T> T selectRow(Class<T> clazz) {
        List<T> rows = selectRows(clazz);
        return rows.isEmpty() ? null : rows.get(0);
    }
}