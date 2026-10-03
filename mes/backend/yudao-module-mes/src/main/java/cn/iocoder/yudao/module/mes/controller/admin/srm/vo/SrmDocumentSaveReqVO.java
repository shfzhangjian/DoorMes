package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM通用业务单据新增/修改 Request VO")
@Data
public class SrmDocumentSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "业务单号不能为空")
    private String docNo;

    private String title;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String status;
    private Long applicantId;
    private String applicantName;
    private String applyDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime applyTime;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private BigDecimal totalScore;
    private String evalGrade;
    private String bizCategory;
    private String bizLevel;
    private String periodType;
    private Integer evalYear;
    private Integer evalQuarter;
    private String payloadJson;
    private String remark;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
