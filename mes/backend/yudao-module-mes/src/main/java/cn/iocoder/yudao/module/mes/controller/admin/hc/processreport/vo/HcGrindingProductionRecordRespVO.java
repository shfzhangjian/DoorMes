package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮生产记录表 Response VO")
@Data
public class HcGrindingProductionRecordRespVO {
    private HcGrindingConsumptionVO consumption;


    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "磨皮明细ID")
    private Long detailId;

    @Schema(description = "磨皮次数编码")
    private String passType;

    @Schema(description = "完工日期的日期部分（兼容字段）")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @Schema(description = "设备ID（寿命快照隔离键，列表默认不展示）")
    private Long equipmentId;

    @Schema(description = "设备编码快照（列表默认不展示）")
    private String equipmentCode;

    @Schema(description = "设备名称快照（列表默认不展示）")
    private String equipmentName;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "垫型快照：BLACK_PAD/WHITE_PAD；为空表示未归类")
    private String padType;

    @Schema(description = "料号")
    private String materialCode;

    @Schema(description = "生产记录角色：FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND")
    private String recordRole;

    @Schema(description = "来源业务类型：FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND")
    private String sourceBizType;

    @Schema(description = "加工母批号")
    private String motherBatchNo;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "加工单元：P/Q/R/S/NONE")
    private String segmentMark;

    @Schema(description = "起米位置(m)")
    private BigDecimal startPosition;

    @Schema(description = "对应一磨前置分配记录ID")
    private Long firstAllocationId;

    @Schema(description = "投入米数(m)")
    private BigDecimal inputLength;

    @Schema(description = "产出米数(m)")
    private BigDecimal outputLength;

    @Schema(description = "一次/二次磨皮")
    private String passName;

    @Schema(description = "砂纸累计寿命(m)")
    private BigDecimal sandpaperLife;

    @Schema(description = "砂纸累计天数")
    private Integer sandpaperLifeDays;

    @Schema(description = "砂纸批号")
    private String sandpaperBatchNo;

    @Schema(description = "砂纸是否物理更换")
    private Boolean sandpaperChanged;

    @Schema(description = "砂纸更换原因")
    private String sandpaperReplaceReason;

    @Schema(description = "导布累计寿命(次)")
    private Integer guideClothLife;

    @Schema(description = "导布批号")
    private String guideClothBatchNo;

    @Schema(description = "导布是否物理更换")
    private Boolean guideClothChanged;

    @Schema(description = "导布更换原因")
    private String guideClothReplaceReason;

    @Schema(description = "更换原因")
    private String replaceReason;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "完工日期，精确到秒")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private String status;
    private String sourceType;

    @Schema(description = "来源磨皮报工明细ID")
    private Long sourceDetailId;

    @Schema(description = "同一来源报工的砂纸记录拆分序号：1=更换前/唯一记录，2=更换后新砂纸")
    private Integer sourceSegmentNo;

    @Schema(description = "手工研发记录发生砂纸更换时的双记录归属键")
    private String manualSplitGroupNo;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
