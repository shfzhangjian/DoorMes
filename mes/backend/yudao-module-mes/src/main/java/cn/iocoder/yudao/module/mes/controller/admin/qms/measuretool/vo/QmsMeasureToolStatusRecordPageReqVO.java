package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 量检具状态调整记录分页 Request VO")
@Data
public class QmsMeasureToolStatusRecordPageReqVO extends PageParam {

    @Schema(description = "量检具台账ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "量检具台账ID不能为空")
    private Long ledgerId;

}
