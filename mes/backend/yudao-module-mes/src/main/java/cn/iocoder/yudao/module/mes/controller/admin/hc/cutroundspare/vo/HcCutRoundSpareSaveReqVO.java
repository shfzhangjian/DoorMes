package cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 裁切备件保存 Request VO")
@Data
public class HcCutRoundSpareSaveReqVO {

    private Long id;

    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;

    @NotBlank(message = "备件类型不能为空")
    private String spareType;

    private Boolean correctHistory;
    private Integer useCount;
    private Integer limitCount;
    private Integer limitDays;
    private String status;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime lastReplaceTime;

    private String remark;
}
