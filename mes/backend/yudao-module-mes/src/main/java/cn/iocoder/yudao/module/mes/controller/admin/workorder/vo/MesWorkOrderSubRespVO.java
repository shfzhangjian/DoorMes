package cn.iocoder.yudao.module.mes.controller.admin.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 派工细单 Response VO")
@Data
public class MesWorkOrderSubRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "6001")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "派工细单号", example = "WO_MTS_01_A_010")
    @ExcelProperty("派工单号")
    private String subOrderNo;

    // ========== 关联父级 ==========
    @Schema(description = "关联主工单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    @ExcelProperty("主工单ID")
    private Long workOrderId;

    @Schema(description = "工单号（冗余）", example = "WO_MTS_01_A")
    @ExcelProperty("工单号")
    private String workOrderNo;

    // ========== 产品信息 (冗余) ==========
    @Schema(description = "产品ID", example = "1002")
    private Long productId;

    @Schema(description = "产品编码", example = "M_SLIT_ROLL")
    @ExcelProperty("产品编码")
    private String productCode;

    @Schema(description = "产品名称", example = "分切子卷_1000m")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "产品规格", example = "1000m*50um")
    @ExcelProperty("产品规格")
    private String productSpec;

    // ========== 工艺路径信息 ==========
    @Schema(description = "关联工艺路线工序ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "901")
    private Long routeProcessId;

    @Schema(description = "工序编码（冗余）", example = "PROCESS_DIE_CUT")
    @ExcelProperty("工序编码")
    private String processCode;

    @Schema(description = "工序名称（冗余）", example = "模切")
    @ExcelProperty("工序名称")
    private String processName;

    @Schema(description = "工序流转顺序号", example = "10")
    @ExcelProperty("顺序号")
    private Integer seqNo;

    // ========== 资源工位 ==========
    @Schema(description = "指定工位ID", example = "9001")
    private Long stationId;

    // 🚨 根据用户指示强制添加 stationCode
    @Schema(description = "工位编码（冗余）", example = "EQ_DIE_01")
    @ExcelProperty("工位编码")
    private String stationCode;

    @Schema(description = "工位名称（冗余）", example = "模切工位A")
    @ExcelProperty("工位名称")
    private String stationName;

    // ========== 计划与实绩 ==========
    @Schema(description = "计划排产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.0")
    @ExcelProperty("计划数量")
    private BigDecimal planQty;

    @Schema(description = "实际产出数量")
    @ExcelProperty("实际产出")
    private BigDecimal actualQty;

    @Schema(description = "良品数量")
    @ExcelProperty("良品数")
    private BigDecimal goodQty;

    @Schema(description = "废品数量")
    @ExcelProperty("废品数")
    private BigDecimal scrapQty;

    @Schema(description = "计划日期", example = "2026-02-16")
    @ExcelProperty("计划日期")
    private LocalDate planDate;

    @Schema(description = "计划开工时间")
    @ExcelProperty("计划开工")
    private LocalDateTime startTime;

    @Schema(description = "计划完工时间")
    @ExcelProperty("计划完工")
    private LocalDateTime endTime;

    @Schema(description = "实际开工时间")
    @ExcelProperty("实际开工")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际完工时间")
    @ExcelProperty("实际完工")
    private LocalDateTime actualEndTime;

    // ========== 状态与人员 ==========
    @Schema(description = "执行操作人")
    @ExcelProperty("操作人")
    private String operatorUser;

    @Schema(description = "状态", example = "PENDING")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat("mes_sub_order_status")
    private String status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
