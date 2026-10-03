package cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 终端工位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcTerminalRespVO {

    @Schema(description = "终端编码")
    @ExcelProperty("终端编码")
    private String terminalCode;

    @Schema(description = "终端名称")
    @ExcelProperty("终端名称")
    private String terminalName;

    @Schema(description = "工作中心ID")
    @ExcelProperty("工作中心ID")
    private Long workCenterId;

    @Schema(description = "工作中心编码")
    @ExcelProperty("工作中心编码")
    private String workCenterCode;

    @Schema(description = "终端模式")
    @ExcelProperty("终端模式")
    private String terminalMode;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}