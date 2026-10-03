package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 温湿度录入看板 Response VO")
@Data
@Builder
public class QmsEnvironmentBoardRespVO {

    private String workshopCode;
    private String workshopName;
    private String recordMonth;
    private Integer dayCount;
    private Standard standard;
    private Summary summary;
    private List<Record> records;

    @Data
    @Builder
    public static class Standard {
        private Long id;
        private String workshopCode;
        private String workshopName;
        private BigDecimal temperatureMin;
        private BigDecimal temperatureMax;
        private BigDecimal humidityMin;
        private BigDecimal humidityMax;
        private String remark;
    }

    @Data
    @Builder
    public static class Summary {
        private Integer recordedDays;
        private Integer confirmedDays;
        private Integer abnormalDays;
        private Integer pendingConfirmDays;
    }

    @Data
    @Builder
    public static class Record {
        private Long id;
        private Integer day;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate recordDate;
        private BigDecimal temperatureValue;
        private BigDecimal humidityValue;
        private String temperatureStatus;
        private String humidityStatus;
        private String overallStatus;
        private String recordStatus;
        private Long recorderId;
        private String recorderUsername;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recordTime;
        private Long confirmerId;
        private String confirmerUsername;
        private String confirmerName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime confirmTime;
        private String confirmStatus;
        private String remark;
    }
}
