/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.executor.sql.execute.result;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.shardingsphere.core.merger.QueryResult;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Distinct query result.
 *
 * @author panjuan
 */
public final class DistinctQueryResult implements QueryResult {

    private final Collection<QueryResult> queryResults;
    
    private final Multimap<String, Integer> columnLabelAndIndexMap;
    
    private final Iterator<List<Object>> resultData;
    
    private List<Object> currentRow;

    public DistinctQueryResult(final Collection<QueryResult> queryResults) {
        this.queryResults = queryResults;
        columnIndexAndDistinctValues = getColumnIndexAndDistinctValues(queryResults);
    }
    
    private Multimap<String, Integer> getColumnLabelAndIndexMap(final QueryResult queryResult) throws SQLException {
        Multimap<String, Integer> result = HashMultimap.create();
        for (int columnIndex = 1; columnIndex <= resultSetMetaData.getColumnCount(); columnIndex++) {
            result.put(resultSetMetaData.getColumnLabel(columnIndex), columnIndex);
        }
        return result;
    }
    
    private Iterator<List<Object>> getResultData(final Collection<QueryResult> queryResults) throws SQLException {
        Set<List<Object>> result = new LinkedHashSet<>();
        for (QueryResult each : queryResults) {
            while (each.next()) {
                List<Object> row = new ArrayList<>(each.getColumnCount());
                for (int columnIndex = 1; columnIndex <= each.getColumnCount(); columnIndex++) {
                    row.add(each.getValue(columnIndex, Object.class));
                }
                result.add(row);
            }
        }
        return result.iterator();
    }
    
    @SneakyThrows
    private Map<Integer, Set<Object>> getColumnIndexAndDistinctValues(final Collection<QueryResult> queryResults) {
        Map<Integer, Set<Object>> result = createColumnIndexAndDistinctValues(queryResults);
        for (QueryResult each : queryResults) {
            fillInColumnIndexAndDistinctValues(result, each);
        }
        return result;
    }
    
    private Map<Integer, Set<Object>> createColumnIndexAndDistinctValues(final Collection<QueryResult> queryResults) throws SQLException {
        Map<Integer, Set<Object>> result = new LinkedHashMap<>(queryResults.iterator().next().getColumnCount());
        for (int i = 1; i <= queryResults.iterator().next().getColumnCount(); i++) {
            result.put(i, new LinkedHashSet<>());
        }
        return result;
    }
    
    @SneakyThrows
    private void fillInColumnIndexAndDistinctValues(final Map<Integer, Set<Object>> columnIndexAndDistinctValues, final QueryResult queryResult) {
        while (queryResult.next()) {
            for (int i = 1; i <= columnIndexAndDistinctValues.size(); i++) {
                columnIndexAndDistinctValues.get(i).add(queryResult.getValue(i, Object.class));
            }
        }
    }
    
    @Override
    public boolean next() {
        if (resultData.hasNext()) {
            currentRow = resultData.next();
            return true;
        }
        currentRow = null;
        return false;
    }

    @Override
    public Object getValue(final int columnIndex, final Class<?> type) {
        return currentRow.get(columnIndex - 1);
    }

    @Override
    public Object getValue(final String columnLabel, final Class<?> type) {
        return currentRow.get(getIndexByColumnLabel(columnLabel));
    }

    @Override
    public Object getCalendarValue(final int columnIndex, final Class<?> type, final Calendar calendar) {
        return currentRow.get(columnIndex - 1);
    }

    @Override
    public Object getCalendarValue(final String columnLabel, final Class<?> type, final Calendar calendar) {
        return currentRow.get(getIndexByColumnLabel(columnLabel));
    }

    @Override
    public InputStream getInputStream(final int columnIndex, final String type) {
        return (InputStream) currentRow.get(columnIndex - 1);
    }

    @Override
    public InputStream getInputStream(final String columnLabel, final String type) {
        return (InputStream) currentRow.get(getIndexByColumnLabel(columnLabel));
    }

    @Override
    public boolean wasNull() {
        return null == currentRow;
    }

    @Override
    public int getColumnCount() {
        return columnLabelAndIndexMap.size();
    }

    @Override
    public String getColumnLabel(final int columnIndex) throws SQLException {
        for (Entry<String, Integer> entry : columnLabelAndIndexMap.entries()) {
            if (columnIndex == entry.getValue()) {
                return entry.getKey();
            }
        }
        throw new SQLException("Column index out of range", "9999");
    }
}
