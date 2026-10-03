package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM供方评审计划执行分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmSupplierReviewExecutionPageReqVO extends PageParam {

    private Integer planYear;

    @Min(value = 1, message = "月份必须在1到12之间")
    @Max(value = 12, message = "月份必须在1到12之间")
    private Integer planMonth;

    private String supplierCode;
    private String supplierName;
    private String materialCode;

    /**
     * 单状态过滤（兼容原有接口），与 executionStatuses 同时存在时取并集。
     */
    private String executionStatus;

    /**
     * 多状态过滤（如 默认 ['EXECUTING','ARCHIVED']）。
     * Spring GET 绑定重复参数：?executionStatuses=EXECUTING&executionStatuses=ARCHIVED
     */
    private List<String> executionStatuses;

    /**
     * 查询范围：MINE-我相关的计划（默认）；ALL-全部计划（需权限 mes:srm-audit-review-record:view-all）
     */
    private String scope;

}
