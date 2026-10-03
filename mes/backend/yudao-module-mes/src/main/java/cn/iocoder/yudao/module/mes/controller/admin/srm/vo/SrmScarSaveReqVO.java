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

@Schema(description = "管理后台 - SRM供方异常与整改台账新增/修改 Request VO")
@Data
public class SrmScarSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "台账编号；服务端保存时生成，更新时不可修改")
    private String scarNo;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商代码")
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料代码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "物料型号")
    private String materialModel;

    @Schema(description = "物料批次")
    private String batchNo;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "异常发生日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @Schema(description = "异常描述")
    private String issueDesc;

    @Schema(description = "异常回复日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate replyDate;

    @Schema(description = "异常回复说明")
    private String replyDesc;

    @Schema(description = "状态；服务端自动管理：有待回复内容则为 CLOSED，否则 WAIT_SUPPLIER")
    private String status;

    @Schema(description = "登记人ID；服务端按当前登录人生成")
    private Long applicantId;

    @Schema(description = "登记人；服务端按当前登录人生成")
    private String applicantName;

    @Schema(description = "登记时间；服务端生成，更新时保持原值")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime applyTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
