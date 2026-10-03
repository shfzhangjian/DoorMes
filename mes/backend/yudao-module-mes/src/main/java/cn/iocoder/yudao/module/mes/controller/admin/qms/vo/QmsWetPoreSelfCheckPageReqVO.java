package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 湿法泡孔自检分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsWetPoreSelfCheckPageReqVO extends PageParam {

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "母批批号")
    private String motherBatchNo;

    @Schema(description = "产品料号")
    private String productMaterialCode;

    @Schema(description = "泡孔自检结果")
    private String selfCheckResult;

    @Schema(description = "图片状态：UPLOADED 已上传，MISSING 未上传")
    private String imageStatus;

    @Schema(description = "泡孔自检时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] selfCheckTime;

}
