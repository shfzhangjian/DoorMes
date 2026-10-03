package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲片号统计同步 Request VO")
@Data
public class QmsCmpWarpageSliceStatSyncReqVO {

    @Schema(description = "需要同步的镜像记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择需要同步的记录")
    private List<Long> ids;
}
