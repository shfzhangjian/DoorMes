package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板原有母批开工完工同步 Request VO")
@Data
public class HcRoughConsoleOriginalMotherTimeStampReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    /** START 或 END；实际时间仅由服务端生成，END 仅允许首次写入原工序报工结束时间。 */
    @NotBlank(message = "同步动作不能为空")
    private String action;

    /** 前端身份签核确认的操作人；未传时兼容使用当前登录人。 */
    private Long operatorId;

    /** 前端身份签核确认的操作人名称；未传时兼容使用当前登录人。 */
    private String operatorName;

}
