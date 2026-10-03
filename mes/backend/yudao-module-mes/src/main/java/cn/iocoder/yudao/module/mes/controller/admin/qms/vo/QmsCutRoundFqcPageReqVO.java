package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 裁切成品检验分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCutRoundFqcPageReqVO extends PageParam {

    private String fqcNo;
    private String taskNo;
    private String planNo;
    private String productionBatchNo;
    private String materialCode;
    private String productModel;
    private String status;
    private String judgment;
    private Boolean recheckFlag;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] submissionTime;
}
