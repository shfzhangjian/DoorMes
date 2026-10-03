package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 角色检验标准范围 Response VO")
@Data
public class QmsQualityStandardRoleScopeRespVO {

    @Schema(description = "范围记录ID；候选列表为空")
    private Long id;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "范围类型")
    private String scopeType;

    @Schema(description = "检验标准ID")
    private Long standardId;

    @Schema(description = "检验标准适用环节")
    private String standardApplyType;

    @Schema(description = "标准编号")
    private String standardNo;

    @Schema(description = "标准名称")
    private String standardName;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "生产类型")
    private String prodTypeName;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "启停状态")
    private Integer status;

    @Schema(description = "审核状态")
    private Integer auditStatus;

    @Schema(description = "范围创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
