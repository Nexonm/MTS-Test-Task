package dev.nexonm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ETL processing result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETLReport {
    private String fileName;
    private ETLStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalRecords;
    private LocalDate fileDate;
    private List<ValidationError> validationErrors;
    private String errorMessage;


    /**
     * Will be added to the JSON response as "durationMillis" field
     * @return
     */
    public long getDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }


    @Override
    public String toString() {
        return "ETLReport{" +
                "fileName='" + fileName + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", totalRecords=" + totalRecords +
                ", fileDate=" + fileDate +
                ", validationErrors=" + validationErrors.size() +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    /**
     * ETL processing status
     */
    public enum ETLStatus {
        COMPLETED,  // Successfully completed
        FAILED      // Failed with error
    }
}


