package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 供应商脱敏字段 Response VO")
@Data
public class SrmSupplierMaskFieldRespVO {

    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private Boolean maskEnabled;
    private Integer sort;
    private String remark;
    private Integer version;

}
