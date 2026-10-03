package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 量检具校准记录分页 Request VO")
@Data
public class QmsMeasureToolCalibrationRecordPageReqVO extends PageParam {

    private Long ledgerId;
    private String recordNo;
    private String toolCode;
    private String toolName;
    private Long categoryId;
    private String usingDepartment;
    private String keeperName;
    private String calibrationResult;
    private String sourceType;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] calibrationDate;

}
