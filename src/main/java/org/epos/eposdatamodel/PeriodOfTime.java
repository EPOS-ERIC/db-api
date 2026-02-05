package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;
import utilities.ParseLocalDateTime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Objects;

/**
 * An interval of time that is named or defined by its start and end dates.
 */
public class PeriodOfTime extends EPOSDataModelEntity{

    /**
     * This property contains the END of the period
     **/
    @Schema(name = "startDate", description = "This property contains the END of the period", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime startDate;

    /**
     * This property contains the END of the period
     **/
    @Schema(name = "endDate", description = "This property contains the END of the period", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime endDate;

    public PeriodOfTime startDate(LocalDateTime startDate) {
        this.startDate = startDate;
        return this;
    }

    /**
     * Get startDate
     *
     * @return startDate
     **/

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setStartDate(String startDate){
        if(!startDate.isEmpty() && !startDate.isBlank())
            this.startDate = ParseLocalDateTime.parse(startDate);
    }

    public PeriodOfTime endDate(LocalDateTime endDate) {
        this.endDate = endDate;
        return this;
    }

    /**
     * Get endDate
     *
     * @return endDate
     **/

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setEndDate(String endDate){
        if(!endDate.isEmpty() && !endDate.isBlank())
            this.endDate = ParseLocalDateTime.parse(endDate);
    }

    @Override
    public String toString() {
        return "PeriodOfTime{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodOfTime that = (PeriodOfTime) o;
        return Objects.equals(getStartDate(), that.getStartDate()) && Objects.equals(getEndDate(), that.getEndDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartDate(), getEndDate());
    }
}
