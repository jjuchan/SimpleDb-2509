package com.back.simpleDb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Sql {

    private SimpleDb simpleDb;
    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object ... values) {
        query.append(sql).append(" ");
        if(values != null) {
            for(Object value : values) {
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
}
