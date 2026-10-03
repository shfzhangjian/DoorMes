package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工序耗材字典新增/修改 Request VO")
@Data
public class HcToolingProcessConsumableSaveReqVO {

    private Long id;

    @NotBlank(message = "工序不能为空")
    private String processCode;

    private String processName;

    @NotBlank(message = "耗材种类不能为空")
    private String consumableType;

    private String consumableTypeName;

    private String defaultErpMaterialCode;

    private String defaultBatchNo;

    private Long defaultUomId;

    private String defaultUomCode;

    private String defaultUomName;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
