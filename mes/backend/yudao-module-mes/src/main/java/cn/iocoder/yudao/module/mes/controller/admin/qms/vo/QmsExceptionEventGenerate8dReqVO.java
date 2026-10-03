package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS异常事件生成8D Request VO")
@Data
public class QmsExceptionEventGenerate8dReqVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long id;

    @Schema(description = "要求结案日")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @Schema(description = "8D团队初值")
    private List<QmsException8dTeamMemberReqVO> teamMembers;

    @Schema(description = "生成说明")
    private String remark;
}
