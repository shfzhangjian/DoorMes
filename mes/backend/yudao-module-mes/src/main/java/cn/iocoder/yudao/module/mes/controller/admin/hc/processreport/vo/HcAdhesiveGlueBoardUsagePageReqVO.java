package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶胶板边料分页 Request VO")
@Data
public class HcAdhesiveGlueBoardUsagePageReqVO extends PageParam {

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批号")
    private String glueBoardBatchNo;

    @Schema(description = "领用状态")
    private String usageStatus;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "记录日期")
    private LocalDate recordDate;
}
