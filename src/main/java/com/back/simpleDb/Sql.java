package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sql {

    private SimpleDb simpleDb;
    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... values) {
        query.append(sql).append(" ");
        if (values != null) {
            for (Object value : values) {
                params.add(value);
            }
        }
        return this;
    }

    public long insert() {
        return simpleDb.insert(query.toString(), params.toArray());
    }

    public int update() {
        return simpleDb.update(query.toString(), params.toArray());
    }

    public int delete() {
        return simpleDb.delete(query.toString(), params.toArray());
    }

    public List<Map<String, Object>> selectRows() {
        return simpleDb.selectRows(query.toString(), params.toArray());
    }

    public Map<String, Object> selectRow() {
        return simpleDb.selectRow(query.toString(), params.toArray());
    }

    public LocalDateTime selectDatetime() {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        return (LocalDateTime) row.values().iterator().next();
    }

    public Long selectLong() {
        Map<String, Object> row = selectRow();
        if (row == null) return null;

        Object value = row.values().iterator().next();

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public String selectString() {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        return (String) row.values().iterator().next();
    }

    public Boolean selectBoolean() {
        Map<String, Object> row = selectRow();
        if (row == null) return null;

        Object value = row.values().iterator().next();

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value != null) {
            return Boolean.parseBoolean(value.toString());
        } else {
            return null;
        }
    }

}
