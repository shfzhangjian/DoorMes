package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 缺陷分类分析查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDefectCategoryAnalysisReqVO extends PageParam {

    @Schema(description = "开始日期", example = "2026-08-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-08-07")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDate;

    @Schema(description = "工序编码：SLITTING/PRESS_SLOT/ADHESIVE2/CUT_ROUND")
    private String processCode;

    @Schema(description = "检验类型：SELF_CHECK/SUBMISSION/FINAL_INSPECTION")
    private String inspectionType;

    @Schema(description = "产品型号或型号前三位")
    private String modelCode;

    @Schema(description = "母卷批号")
    private String motherRollBatchNo;

    @Schema(description = "分段批号")
    private String segmentBatchNo;

    @Schema(description = "扫码确认片号")
    private String pieceNo;

    @Schema(description = "缺陷分类")
    private String defectCategory;

    @Schema(description = "综合关键字")
    private String keyword;

    @Schema(description = "汇总层级：MOTHER/SEGMENT")
    private String groupLevel;
}
