package com.px.ifp.spc.service.iot;

import com.px.ifp.spc.dto.TdengineScadaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TdengineScadaQueryServiceImpl {
    private static final String SUPER_TABLE = "measure_tsdb.sensor_data";
    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
            "temperature", "humidity", "pressure", "voltage", "current"
    ));
    private static final Set<String> ALLOWED_AGGREGATES = new HashSet<>(Arrays.asList(
            "first", "last", "max", "min", "avg", "var_pop", "stddev_pop"
    ));

    @Autowired
    @Qualifier("tdengineJdbcTemplate")
    private JdbcTemplate tdengineJdbcTemplate;

    public List<Map<String, Object>> queryScadaIndicator(TdengineScadaQuery query) {
        List<String> fields = normalizeFields(query.getFields());
        String aggregate = normalizeAggregate(query.getAggregateFunction());
        String interval = normalizeInterval(query.getInterval());

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(buildSelectClause(fields, aggregate, interval));
        sql.append(" FROM ").append(SUPER_TABLE);

        List<String> where = new ArrayList<>();
        if (query.getStartTime() != null && !query.getStartTime().isEmpty()) {
            where.add("ts >= ?");
            params.add(query.getStartTime());
        }
        if (query.getEndTime() != null && !query.getEndTime().isEmpty()) {
            where.add("ts <= ?");
            params.add(query.getEndTime());
        }

        List<String> tbNames = normalizeTbNames(query.getTbNames());
        if (!tbNames.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(tbNames.size(), "?"));
            where.add("tbname IN (" + placeholders + ")");
            params.addAll(tbNames);
        }

        if (query.getDeviceId() != null && !query.getDeviceId().isEmpty()) {
            where.add("device_id = ?");
            params.add(query.getDeviceId());
        }

        if (query.getLocation() != null && !query.getLocation().isEmpty()) {
            where.add("location = ?");
            params.add(query.getLocation());
        }

        Map<String, Object> featureEquals = query.getFeatureEquals();
        if (featureEquals != null) {
            for (Map.Entry<String, Object> entry : featureEquals.entrySet()) {
                if (!ALLOWED_FIELDS.contains(entry.getKey())) {
                    continue;
                }
                where.add(entry.getKey() + " = ?");
                params.add(entry.getValue());
            }
        }

        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }

        if (query.isPartitionByTbname()) {
            sql.append(" PARTITION BY tbname");
        }

        if (interval != null) {
            sql.append(" INTERVAL(").append(interval).append(")");
        }

        return tdengineJdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private List<String> normalizeFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return new ArrayList<>(ALLOWED_FIELDS);
        }
        return fields.stream()
                .filter(ALLOWED_FIELDS::contains)
                .collect(Collectors.toList());
    }

    private String normalizeAggregate(String aggregate) {
        if (aggregate == null) {
            return null;
        }
        String lower = aggregate.trim().toLowerCase();
        return ALLOWED_AGGREGATES.contains(lower) ? lower : null;
    }

    private String normalizeInterval(String interval) {
        if (interval == null || interval.trim().isEmpty()) {
            return null;
        }
        String trimmed = interval.trim();
        if (trimmed.matches("^[0-9]+[a-zA-Z]+$")) {
            return trimmed;
        }
        return null;
    }

    private List<String> normalizeTbNames(List<String> tbNames) {
        if (tbNames == null || tbNames.isEmpty()) {
            return Collections.emptyList();
        }
        return tbNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String buildSelectClause(List<String> fields, String aggregate, String interval) {
        List<String> selectParts = new ArrayList<>();
        if (interval != null) {
            selectParts.add("_wstart AS ts");
        }

        if (aggregate == null) {
            selectParts.addAll(fields);
        } else {
            for (String field : fields) {
                selectParts.add(aggregate + "(" + field + ") AS " + aggregate + "_" + field);
            }
        }

        return String.join(", ", selectParts);
    }

    public Map<String, Object> generateMockSensorData(String sensorId, String location, int count, int intervalSeconds) {
        if (sensorId == null || sensorId.trim().isEmpty()) {
            sensorId = "sensor_001";
        }
        if (location == null || location.trim().isEmpty()) {
            location = "Factory-A";
        }
        if (count <= 0 || count > 1000) {
            count = 60;
        }
        if (intervalSeconds <= 0) {
            intervalSeconds = 1;
        }

        String subTableName = "d_" + sensorId;
        Random random = new Random();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            String createSubTableSql = String.format(
                    "CREATE TABLE IF NOT EXISTS measure_tsdb.%s USING measure_tsdb.sensor_data TAGS ('%s', '%s')",
                    subTableName, sensorId, location
            );
            tdengineJdbcTemplate.execute(createSubTableSql);

            LocalDateTime startTime = LocalDateTime.now().minusSeconds(count * intervalSeconds);
            List<String> insertStatements = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                LocalDateTime timestamp = startTime.plusSeconds(i * intervalSeconds);
                float temperature = 20.0f + random.nextFloat() * 10.0f;
                float humidity = 40.0f + random.nextFloat() * 20.0f;
                float pressure = 100.0f + random.nextFloat() * 10.0f;
                float voltage = 220.0f + random.nextFloat() * 10.0f;
                float current = 5.0f + random.nextFloat() * 5.0f;

                String insertSql = String.format(
                        "INSERT INTO measure_tsdb.%s (ts, temperature, humidity, pressure, voltage, current) VALUES ('%s', %.2f, %.2f, %.2f, %.2f, %.2f)",
                        subTableName,
                        timestamp.format(formatter),
                        temperature,
                        humidity,
                        pressure,
                        voltage,
                        current
                );
                insertStatements.add(insertSql);
            }

            for (String sql : insertStatements) {
                tdengineJdbcTemplate.execute(sql);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("subTableName", subTableName);
            result.put("sensorId", sensorId);
            result.put("location", location);
            result.put("insertedCount", count);
            result.put("intervalSeconds", intervalSeconds);
            result.put("message", String.format("成功生成 %d 条模拟数据到子表 %s", count, subTableName));

            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
}
