package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 压槽表单填写记录 Response VO")
@Data
public class HcPressSlotFormRecordRespVO {

    @Schema(description = "前端行 ID")
    private String id;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源 ID")
    private Long sourceId;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "压槽片号")
    private String pressSlotSliceNo;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "设备 ID")
    private Long equipmentId;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "记录日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "填写人")
    private String fillUserName;

    @Schema(description = "确认人")
    private String confirmUserName;

    @Schema(description = "填写时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime fillTime;

    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "详情渲染载荷")
    private Map<String, Object> payload;
}
