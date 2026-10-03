package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - NCR 处置执行通知分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsNcDispositionNotifyPageReqVO extends PageParam {

    @Schema(description = "NCR 单号")
    private String ncNo;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "通知状态：PENDING 待回复、REPLIED 已回复")
    private String notifyStatus;

}
