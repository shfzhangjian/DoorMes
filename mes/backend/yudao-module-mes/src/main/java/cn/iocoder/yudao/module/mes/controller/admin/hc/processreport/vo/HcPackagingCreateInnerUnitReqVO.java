package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 创建内包装单元 Request VO")
@Data
public class HcPackagingCreateInnerUnitReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotNull(message = "包装规格不能为空")
    private Integer packageSpec;

    private Boolean backfillFlag;
    private String backfillReason;
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime actualWorkTime;
    private String recorderName;
    private String remark;
}
