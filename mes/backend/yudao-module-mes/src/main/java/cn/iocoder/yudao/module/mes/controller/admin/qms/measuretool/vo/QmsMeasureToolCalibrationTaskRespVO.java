package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 量检具校准预警任务 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsMeasureToolCalibrationTaskRespVO {

    private Long id;

    @ExcelProperty(value = "任务号", index = 0)
    private String taskNo;

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
    @ExcelProperty(value = "应校准日期", index = 6)
    private LocalDate dueDate;

    @ExcelProperty(value = "预警状态", index = 7)
    private String warningStatus;

    @ExcelProperty(value = "任务状态", index = 8)
    private String taskStatus;

    @ExcelProperty(value = "来源", index = 9)
    private String sourceType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;

    private Integer warningDays;
    private Long recordId;
    private String handlerName;
    private String remark;
    private Integer version;

}
