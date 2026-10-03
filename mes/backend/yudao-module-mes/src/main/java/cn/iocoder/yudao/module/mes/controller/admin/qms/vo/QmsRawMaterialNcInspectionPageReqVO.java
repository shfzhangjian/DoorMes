package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 原物料不合格处置单来源检验分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsRawMaterialNcInspectionPageReqVO extends PageParam {

    @Schema(description = "检验类型：IQC")
    private String inspectionType;

    @Schema(description = "检验单号")
    private String inspectionNo;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "NCR生成状态：ALL/GENERATED/PENDING")
    private String ncrStatus;

    @Schema(description = "检验时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectionTime;
}
