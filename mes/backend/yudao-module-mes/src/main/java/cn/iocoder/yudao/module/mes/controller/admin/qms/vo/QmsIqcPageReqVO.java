package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - IQC进料检验单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsIqcPageReqVO extends PageParam {

    @Schema(description = "IQC检验单号")
    private String iqcNo;

    @Schema(description = "收料单号")
    private String receiptNo;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "检验标准编号")
    private String standardNo;

    @Schema(description = "标准匹配模式")
    private String standardMatchMode;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "是否复检单")
    private Boolean recheckFlag;

    @Schema(description = "留样状态；字典:mes_fai_retention_status")
    private String retentionStatus;

    @Schema(description = "留样销毁状态；字典:mes_fai_retention_destroy_status")
    private String retentionDestroyStatus;

    @Schema(description = "只查询未过期留样记录")
    private Boolean retentionActiveOnly;

    @Schema(description = "只查询已过期留样记录")
    private Boolean retentionExpiredOnly;

    @Schema(description = "检验时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectionTime;

    @Schema(description = "留样过期时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] retentionExpireTime;
}
