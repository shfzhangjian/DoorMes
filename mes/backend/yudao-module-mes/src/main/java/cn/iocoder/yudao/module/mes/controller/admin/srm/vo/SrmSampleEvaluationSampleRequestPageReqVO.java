package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM样品评价选择样品需求单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmSampleEvaluationSampleRequestPageReqVO extends PageParam {

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "申请编号")
    private String requestNo;

    @Schema(description = "品名")
    private String materialName;

    @Schema(description = "供应商名称")
    private String supplierName;

}
