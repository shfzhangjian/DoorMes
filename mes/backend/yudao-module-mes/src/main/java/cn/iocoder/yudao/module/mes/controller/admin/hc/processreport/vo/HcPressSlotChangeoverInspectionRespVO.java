package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 压槽首检记录 Response VO")
@Data
public class HcPressSlotChangeoverInspectionRespVO {

    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long sourceSlittingSliceId;
    private String pressSlotSliceNo;
    private String motherSegmentBatchNo;
    private String productionModelCode;
    private String productionMaterialCode;
    private String previousModelCode;
    private String currentPlanNo;
    private String inspectionStatus;
    private String inspectionStatusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime feedbackTime;

    private String feedbackResult;
    private String feedbackRemark;
    private String headerDataJson;
    private String detailItemsJson;
    private String recorderName;
    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;

    private String remark;
    private String extraJson;
    private List<HcAdhesiveCheckItemRespVO> checkItems;
}
