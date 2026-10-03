package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板中间品记录单 Response VO")
@Data
public class HcRoughConsoleMiddleProductRecordRespVO {

    private Long recordId;
    private String formCode;
    private String formName;
    private String passType;
    private String passName;
    private String segmentMark;
    private String segmentName;
    private BigDecimal segmentTotalLength;
    private BigDecimal processLength;
    private String materialCode;
    private String materialName;
    private String motherModelCode;
    private String motherModelName;
    private String motherBatchNo;
    private String productionBatchNo;
    private BigDecimal widthMm;
    private String docStatus;
    private String resultStatus;
    private String headerDataJson;
    private Integer generatedLength;
    private String recorder;
    private String confirmer;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;

    private List<HcRoughConsoleMiddleProductItemRespVO> details;
}
