package cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;

@Schema(description = "管理后台 - 工艺路线新增/修改 Request VO")
@Data
public class HcRouteSaveReqVO {

    @Schema(description = "路线编码")
    @NotBlank(message = "路线编码不能为空")
    private String routeCode;

    @Schema(description = "路线名称")
    @NotBlank(message = "路线名称不能为空")
    private String routeName;

    @Schema(description = "适用范围")
    @NotBlank(message = "适用范围不能为空")
    private String applicableScope;

    @Schema(description = "适用物料ID")
    private Long productMaterialId;

    @Schema(description = "适用物料编码")
    private String productMaterialCode;

    @Schema(description = "适用层级")
    @NotBlank(message = "适用层级不能为空")
    private String productLevel;

    @Schema(description = "版本号")
    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    @Schema(description = "路线类型")
    private String routeType;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "路线工序列表")
    private List<HcRouteOperationDO> routeOperations;

}
