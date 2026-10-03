package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - CMP软垫翘曲片号统计分页 Request VO")
@Data
public class QmsCmpWarpageSliceStatPageReqVO extends PageParam {

    @Schema(description = "关键词：母批/分段片号/生产片号/客户片号/发货需求单")
    private String keyword;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "母批号")
    private String parentBatchNo;

    @Schema(description = "分段片号")
    private String segmentSliceNo;

    @Schema(description = "检测项判定：OK/NG/PENDING（待判定）")
    private String inspectionResult;

    @Schema(description = "是否人工覆盖实际值")
    private Boolean manualOverride;

    @Schema(description = "统计日期开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDateStart;

    @Schema(description = "统计日期结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDateEnd;
}
