package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - SRM供方异常与整改台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmScarPageReqVO extends PageParam {

    private String scarNo;
    private String supplierCode;
    private String supplierName;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] issueDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] replyDate;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}