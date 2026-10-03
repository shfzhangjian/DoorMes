package cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 表单模板 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcFormTemplateRespVO {

    @Schema(description = "模板编码")
    @ExcelProperty("模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    @ExcelProperty("模板名称")
    private String templateName;

    @Schema(description = "模板类型")
    @ExcelProperty("模板类型")
    private String templateType;

    @Schema(description = "业务工序")
    @ExcelProperty("业务工序")
    private String businessStage;

    @Schema(description = "表单样式")
    @ExcelProperty("表单样式")
    private String formStyle;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}