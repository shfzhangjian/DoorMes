package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 量检具新增申请 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsMeasureToolApplyRespVO {

    private Long id;

    @ExcelProperty(value = "申请单号", index = 0)
    private String applyNo;

    @ExcelProperty(value = "量检具名称", index = 1)
    private String toolName;

    private Long categoryId;

    @ExcelProperty(value = "分类", index = 2)
    private String categoryName;

    @ExcelProperty(value = "型号", index = 3)
    private String model;

    @ExcelProperty(value = "规格", index = 4)
    private String specification;

    @ExcelProperty(value = "精度", index = 5)
    private String accuracy;

    @ExcelProperty(value = "量程", index = 6)
    private String measureRange;

    @ExcelProperty(value = "制造商/品牌", index = 7)
    private String manufacturer;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty(value = "采购日期", index = 8)
    private LocalDate purchaseDate;

    @ExcelProperty(value = "校准周期(月)", index = 9)
    private Integer calibrationCycleMonths;

    @ExcelProperty(value = "使用部门", index = 10)
    private String usingDepartment;

    @ExcelProperty(value = "保管人", index = 11)
    private String keeperName;

    @ExcelProperty(value = "申请部门", index = 12)
    private String applyDepartment;

    @ExcelProperty(value = "申请人", index = 13)
    private String applicantName;

    @ExcelProperty(value = "状态", index = 14)
    private String status;

    @ExcelProperty(value = "分配编码", index = 15)
    private String assignedToolCode;

    private Integer warningDays;
    private String storageLocation;
    private String applyReason;
    private String approvalOpinion;
    private String approvedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime approvedTime;

    private Long ledgerId;
    private String remark;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
