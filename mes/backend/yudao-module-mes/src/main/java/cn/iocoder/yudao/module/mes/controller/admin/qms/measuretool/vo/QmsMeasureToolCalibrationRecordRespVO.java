package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 量检具校准记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsMeasureToolCalibrationRecordRespVO {

    private Long id;

    @ExcelProperty(value = "记录号", index = 0)
    private String recordNo;

    private Long taskId;
    private Long ledgerId;

    @ExcelProperty(value = "量检具编码", index = 1)
    private String toolCode;

    @ExcelProperty(value = "量检具名称", index = 2)
    private String toolName;

    private Long categoryId;

    @ExcelProperty(value = "分类", index = 3)
    private String categoryName;

    @ExcelProperty(value = "使用部门", index = 4)
    private String usingDepartment;

    @ExcelProperty(value = "保管人", index = 5)
    private String keeperName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty(value = "校准日期", index = 6)
    private LocalDate calibrationDate;

    @ExcelProperty(value = "校准方式", index = 7)
    private String calibrationMethod;

    @ExcelProperty(value = "校准机构", index = 8)
    private String calibrationOrg;

    @ExcelProperty(value = "校准人", index = 9)
    private String calibrator;

    @ExcelProperty(value = "校准结果", index = 10)
    private String calibrationResult;

    @ExcelProperty(value = "证书编号", index = 11)
    private String certificateNo;

    private String certificateAttachment;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty(value = "有效期至", index = 12)
    private LocalDate validUntil;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty(value = "下次校准日期", index = 13)
    private LocalDate nextCalibrationDate;

    @ExcelProperty(value = "校准费用", index = 14)
    private BigDecimal cost;

    @ExcelProperty(value = "来源", index = 15)
    private String sourceType;

    private String remark;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
