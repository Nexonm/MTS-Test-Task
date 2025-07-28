package dev.nexonm.mapper;

import dev.nexonm.dto.csv.CSVRecord;
import dev.nexonm.entity.Agent;
import dev.nexonm.entity.CallDetailRecord;
import dev.nexonm.service.CSVFileReader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CallDetailRecordMapper {


    public static CSVRecord mapRowToRecord(String[] row) {
        if (row.length != CSVFileReader.EXPECTED_HEADERS.length) {
            throw new IllegalArgumentException(
                    String.format("Expected %d fields, found %d", CSVFileReader.EXPECTED_HEADERS.length, row.length));
        }
        try {
            return new CSVRecord(
                    row[0] != null ? row[0].trim() : null,  // time
                    row[1] != null ? row[1].trim() : null,  // numberA
                    row[2] != null ? row[2].trim() : null,  // numberB
                    row[3] != null ? Integer.parseInt(row[3].trim()) : null,  // duration
                    row[4] != null ? row[4].trim() : null   // account
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid duration value '%s'", row[3]), e);
        }
    }

    public static CallDetailRecord mapRecordToEntity(String[] row, LocalDate fileDate,
                                                     Map<String, Agent> agentMap) {
        CSVRecord record = mapRowToRecord(row);
        LocalTime time = LocalTime.parse(record.getTime(), DateTimeFormatter.ofPattern("HH:mm:ss"));
        LocalDateTime fullTimestamp = LocalDateTime.of(fileDate, time);
        Agent agent = agentMap.get(record.getAccount());
        if (agent == null) {
            throw new IllegalArgumentException(
                    String.format("Agent not found for account: %s", record.getAccount()));
        }
        return new CallDetailRecord(
                fullTimestamp,
                record.getNumberA(),
                record.getNumberB(),
                record.getDuration(),
                agent
        );
    }

    /**
     * Transforms CSV records into JPA entities
     */
    public static List<CallDetailRecord> mapSCVToEntities(List<String[]> rows, LocalDate fileDate,
                                                Map<String, Agent> agents) {
        List<CallDetailRecord> entities = new ArrayList<>();
        for (String[] row : rows) {
            CallDetailRecord entity = mapRecordToEntity(row, fileDate, agents);
            entities.add(entity);
        }
        return entities;
    }

}
