package com.back.simpleDb;

import lombok.SneakyThrows;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb db;
    private final StringBuilder sb = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb db) {
        this.db = db;
    }

    public Sql append(String frag) {
        if (!sb.isEmpty()) sb.append("\n");
        sb.append(frag);
        return this;
    }

    public Sql append(String sql, Object... values) {
        if (!sb.isEmpty()) sb.append("\n");
        sb.append(sql).append(" ");
        if (values != null) {
            params.addAll(Arrays.asList(values));
        }
        return this;
    }

    @SneakyThrows
    public long insert() {
        try (PreparedStatement ps = db.getConnection().prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    @SneakyThrows
    public int update() {
        try (PreparedStatement ps = db.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate(); // 영향을 받은 row 개수 리턴
        }
    }

    @SneakyThrows
    public int delete() {
        try (PreparedStatement ps = db.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate(); // 삭제된 row 개수 리턴
        }
    }

    @SneakyThrows
    public List<Map<String, Object>> selectRows() {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                var meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);

                        // DATETIME 컬럼을 LocalDateTime으로 변환
                        if (value instanceof Timestamp ts) {
                            value = ts.toLocalDateTime();
                        }

                        row.put(columnName, value);
                    }
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    @SneakyThrows
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        if (rows.isEmpty()) return null;
        return rows.get(0);
    }

    public LocalDateTime selectDatetime() {
        Object value = selectOneValue();
        if (value == null) return null;

        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();

        throw new IllegalStateException("selectDatetime(): Unexpected value type = " + value.getClass());
    }

    @SneakyThrows
    private Object selectOneValue() {
        try (PreparedStatement ps = db.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);

                    // DATETIME → LocalDateTime 변환
                    if (value instanceof Timestamp ts) {
                        return ts.toLocalDateTime();
                    }
                    return value;
                }
            }
        }
        return null;
    }

    public Long selectLong() {
        Object value = selectOneValue();
        return value == null ? null : ((Number) value).longValue();
    }

    public String selectString() {
        Object value = selectOneValue();
        return value == null ? null : value.toString();
    }

    public Boolean selectBoolean() {
        Object value = selectOneValue();
        if (value == null) return null;

        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        if (value instanceof String s) return s.equals("1") || s.equalsIgnoreCase("true");

        return false;
    }


}

