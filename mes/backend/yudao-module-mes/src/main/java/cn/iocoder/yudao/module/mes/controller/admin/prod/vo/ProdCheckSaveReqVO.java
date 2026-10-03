// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.prod.vo.ProdCheckSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.prod.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "移动端 - 生产质检记录保存 Request VO")
@Data
public class ProdCheckSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联排产ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排产ID不能为空")
    private Long scheduleId;

    @Schema(description = "检查类型(PRE_CHECK/SELF_CHECK/IPQC)")
    private String checkType;

    @Schema(description = "检查项目")
    private String checkItem;

    @Schema(description = "标准值")
    private String standardValue;

    @Schema(description = "实测值")
    private String actualValue;

    @Schema(description = "是否合格(true:合格, false:不合格)")
    private Boolean pass; // 🚨 架构师红线：彻底消灭 isPass

    @Schema(description = "检查时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime checkTime;

    @Schema(description = "检查人")
    private String checkUser;

    @Schema(description = "备注")
    private String remark;

}
