package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - OQC出货检验分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsOqcPageReqVO extends PageParam {

    private String oqcNo;
    private String shippingNo;
    private String noticeNo;
    private String customerName;
    private String materialCode;
    private String batchNo;
    private String status;
    private String judgment;
    private Boolean recheckFlag;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectionTime;
}
