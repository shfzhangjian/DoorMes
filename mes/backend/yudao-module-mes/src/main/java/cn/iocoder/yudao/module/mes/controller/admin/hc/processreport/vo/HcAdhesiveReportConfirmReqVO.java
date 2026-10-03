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

@Schema(description = "管理后台 - 粘双面胶报工记录扫码确认 Request VO")
@Data
public class HcAdhesiveReportConfirmReqVO {

    @NotNull(message = "报工记录ID不能为空")
    private Long id;

    @NotBlank(message = "扫码批次号不能为空")
    private String scannedBatchNo;

    /** 是否由粘胶2全量一键扫码确认发起。 */
    private Boolean oneClickBatchConfirm;

    /** 前端预览时获取的实际生产型号，用于拒绝换型后页面过期的批量确认。 */
    private String expectedRuntimeModelCode;

    private String actualSizeRule;

    private String glueBoardModel;

    private String glueBoardMaterialCode;

    private String glueBoardBatchNo;

    private Long glueBoardUsageId;

    private String confirmerName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;
}
