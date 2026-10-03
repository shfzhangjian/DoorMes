package cn.iocoder.yudao.module.mes.controller.admin.workorder.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 派工细单保存 Request VO")
@Data
public class MesWorkOrderSubSaveReqVO {

    @Schema(description = "主键ID", example = "6001")
    private Long id;

    // ========== 关联父级 ==========
    @Schema(description = "关联主工单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    @NotNull(message = "主工单ID不能为空")
    private Long workOrderId;

    @Schema(description = "工单号（冗余）", example = "WO_MTS_01_A")
    private String workOrderNo; // ➕ 新增

    // ========== 产品快照 (新增) ==========
    @Schema(description = "产品ID（冗余）", example = "1002")
    private Long productId; // ➕ 新增
    @Schema(description = "产品编码（冗余）", example = "M_SLIT_ROLL")
    private String productCode; // ➕ 新增
    @Schema(description = "产品名称（冗余）", example = "分切子卷_1000m")
    private String productName; // ➕ 新增
    @Schema(description = "产品规格（冗余）", example = "1000m*50um")
    private String productSpec; // ➕ 新增

    // ========== 工艺路线快照 (新增) ==========
    @Schema(description = "关联工艺路线工序ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "901")
    @NotNull(message = "工序节点ID不能为空")
    private Long routeProcessId;

    @Schema(description = "所属工艺路线ID", example = "202")
    private Long routeId; // ➕ 新增
    @Schema(description = "所属工艺路线编码", example = "RT_FILM_STD")
    private String routeCode; // ➕ 新增

    @Schema(description = "所属标准工艺ID", example = "101")
    private Long processId; // ➕ 新增
    @Schema(description = "工序编码", example = "PROCESS_DIE_CUT")
    private String processCode; // ➕ 新增
    @Schema(description = "工序名称", example = "模切")
    private String processName; // ➕ 新增

    @Schema(description = "派工细单号", example = "WO_MTS_01_A_010")
    private String subOrderNo;

    @Schema(description = "工序流转顺序号", example = "10")
    private Integer seqNo;

    // ========== 资源信息 ==========
    @Schema(description = "指定工位ID", example = "9001")
    private Long stationId;

    @Schema(description = "工位名称（冗余）", example = "模切工位A")
    private String stationName;

    @Schema(description = "工位编号（冗余）", example = "AABCC")
    private String stationCode;

    // ========== 计划与排程 ==========
    @Schema(description = "计划日期", example = "2026-02-16")
    private LocalDate planDate; // ➕ 新增

    @Schema(description = "计划排产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.0")
    @NotNull(message = "计划排产数量不能为空")
    private BigDecimal planQty;

    @Schema(description = "计划开工时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime startTime; // 📝 注意 DO 字段名为 startTime, 之前 VO 可能是 planStartTime

    @Schema(description = "计划完工时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime endTime; // 📝 同上

    // ========== 执行结果 ==========
    @Schema(description = "执行操作人")
    private String operatorUser;

    @Schema(description = "状态", example = "PENDING")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
