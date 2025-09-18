package com.back.simpleDb;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final boolean devMode;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public Sql(SimpleDb simpleDb, boolean devMode) {
        this.simpleDb = simpleDb;
        this.devMode = devMode;
    }

    public Sql append(String sqlPart, Object... args) {
        if (sqlBuilder.length() > 0) sqlBuilder.append(" ");
        sqlBuilder.append(sqlPart);
        if (args != null) Collections.addAll(params, args);
        return this;
    }

    public Sql appendIn(String sqlPart, Object... values) {
        if (values != null && values.length == 1 && values[0] != null && values[0].getClass().isArray()) {
            values = toObjectArray(values[0]);
        }

        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) inClause.append(", ");
            inClause.append("?");
        }

        String sqlWithIn = sqlPart.replace("?", inClause.toString());
        append(sqlWithIn, values);

        return this;
    }

    private Object[] toObjectArray(Object array) {
        int length = java.lang.reflect.Array.getLength(array);
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = java.lang.reflect.Array.get(array, i);
        }
        return result;
    }

    public long insert() {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {
            setParams(pstmt);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeIfNotTransaction(conn);
        }
    }

    public int update() {
        return executeUpdate();
    }

    public int delete() {
        return executeUpdate();
    }

    private int executeUpdate() {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            setParams(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeIfNotTransaction(conn);
        }
    }

    public Long selectLong() {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            setParams(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeIfNotTransaction(conn);
        }
    }

    public Boolean selectBoolean() {
        Long val = selectLong();
        if (val == null) return null;
        return val != 0;
    }

    public String selectString() {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            setParams(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeIfNotTransaction(conn);
        }
    }

    public java.time.LocalDateTime selectDatetime() {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            setParams(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getTimestamp(1).toLocalDateTime();
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeIfNotTransaction(conn);
        }
    }

    public List<Map<String, Object>> selectRows() {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            setParams(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    list.add(row);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeIfNotTransaction(conn);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        if (rows.isEmpty()) return null;
        return rows.get(0);
    }

    public List<Long> selectLongs() {
        List<Map<String, Object>> rows = selectRows();
        List<Long> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object val = row.values().iterator().next();
            if (val instanceof Number) result.add(((Number) val).longValue());
            else result.add(Long.parseLong(val.toString()));
        }
        return result;
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();
        if (row == null) return null;
        return objectMapper.convertValue(row, cls);
    }

    public <T> List<T> selectRows(Class<T> cls) {
        List<Map<String, Object>> rows = selectRows();
        List<T> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(objectMapper.convertValue(row, cls));
        }
        return result;
    }

    private void setParams(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    private Connection getConnection() {
        try {
            return simpleDb.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeIfNotTransaction(Connection conn) {
        try {
            if (conn != null && !simpleDb.isTransactionMode() && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
