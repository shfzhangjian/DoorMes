package cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 货主 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcOwnerRespVO {

    @Schema(description = "货主编码")
    @ExcelProperty("货主编码")
    private String ownerCode;

    @Schema(description = "货主名称")
    @ExcelProperty("货主名称")
    private String ownerName;

    @Schema(description = "货主类型")
    @ExcelProperty("货主类型")
    private String ownerType;

    @Schema(description = "联系人")
    @ExcelProperty("联系人")
    private String contactName;

    @Schema(description = "联系电话")
    @ExcelProperty("联系电话")
    private String contactPhone;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}