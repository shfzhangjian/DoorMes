package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM样品评价新增/修改 Request VO")
@Data
public class SrmSampleEvaluationSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    private Long sampleRequestId;
    private String sampleRequestNo;
    @NotNull(message = "项目不能为空")
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;

    @NotEmpty(message = "品名不能为空")
    private String materialName;
    private String materialModel;

    @DecimalMin(value = "0", inclusive = false, message = "样品数量必须大于 0")
    private BigDecimal sampleQty;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate evaluationDate;

    private Integer sampleSendCount;
    private List<String> verificationTypes;
    private List<String> inspectionTypes;
    private String applyDept;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;

    private Long approvedByUserId;
    private String approvedByName;
    private String remark;

    @Valid
    private List<Item> items;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

    @Data
    public static class Item {
        private Long id;
        private Integer rowNo;
        private String itemName;
        private String technicalRequirement;
        private String testData1;
        private String testData2;
        private String testData3;
        private String testData4;
        private String testData5;
        private String itemJudgement;
        private String itemStatus;
    }

}
