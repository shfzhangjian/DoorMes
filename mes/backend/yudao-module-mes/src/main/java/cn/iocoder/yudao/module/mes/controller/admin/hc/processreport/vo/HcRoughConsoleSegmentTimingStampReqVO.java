package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板母批/分段开工完工打点 Request VO")
@Data
public class HcRoughConsoleSegmentTimingStampReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotBlank(message = "磨皮次数不能为空")
    private String passType;

    @NotBlank(message = "时间记录对象不能为空")
    private String segmentMark;

    /** START 或 END；实际时间仅由服务端生成。 */
    @NotBlank(message = "打点动作不能为空")
    private String action;

    /** 前端身份签核确认的操作人；未传时兼容使用当前登录人。 */
    private Long operatorId;

    /** 前端身份签核确认的操作人名称；未传时兼容使用当前登录人。 */
    private String operatorName;

}
