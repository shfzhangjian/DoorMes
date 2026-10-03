package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲裁切送检片号导入 Response VO")
@Data
@Builder
public class QmsCmpWarpageSliceImportRespVO {

    @Schema(description = "符合条件的去重送检片号数")
    private Integer sourceCount;

    @Schema(description = "本次新增片号数")
    private Integer importedCount;

    @Schema(description = "已存在并跳过片号数")
    private Integer existingCount;

    @Schema(description = "同片号重复送检明细数")
    private Integer duplicateSourceCount;

    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;
}
