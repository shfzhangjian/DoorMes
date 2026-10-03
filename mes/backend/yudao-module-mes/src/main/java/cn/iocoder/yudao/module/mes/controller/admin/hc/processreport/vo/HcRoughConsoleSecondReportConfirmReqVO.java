package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 磨皮看板第二次磨皮扫码确认 Request VO")
@Data
public class HcRoughConsoleSecondReportConfirmReqVO {

    @NotNull(message = "二次磨皮记录ID不能为空")
    private Long id;

    @NotBlank(message = "扫码批次号不能为空")
    private String scannedBatchNo;

    private String confirmerName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;

    @Schema(description = "是否强制入中间边库；默认 false，保留人工入库接口")
    private Boolean forcePostWip;
}
