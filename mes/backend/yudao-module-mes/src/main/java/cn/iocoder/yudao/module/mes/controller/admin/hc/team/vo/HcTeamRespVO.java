package cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 班组 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcTeamRespVO {

    @Schema(description = "班组编码")
    @ExcelProperty("班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
    @ExcelProperty("班组名称")
    private String teamName;

    @Schema(description = "默认工作中心ID")
    @ExcelProperty("默认工作中心ID")
    private Long workCenterId;

    @Schema(description = "默认工作中心编码")
    @ExcelProperty("默认工作中心编码")
    private String workCenterCode;

    @Schema(description = "班组长用户ID")
    @ExcelProperty("班组长用户ID")
    private Long leaderUserId;

    @Schema(description = "班组长姓名")
    @ExcelProperty("班组长姓名")
    private String leaderName;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

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