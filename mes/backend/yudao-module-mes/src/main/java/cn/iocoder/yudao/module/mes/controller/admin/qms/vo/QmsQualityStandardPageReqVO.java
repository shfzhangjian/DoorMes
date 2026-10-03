package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 检验标准分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsQualityStandardPageReqVO extends PageParam {

    @Schema(description = "标准名称或标准编号")
    private String standardName;

    @Schema(description = "关联物料编码")
    private String materialCode;

    @Schema(description = "关联物料名称")
    private String materialName;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号编码或名称")
    private String productModelKeyword;

    @Schema(description = "工序ID")
    private Long processId;

    @Schema(description = "工序编码或名称")
    private String processKeyword;

    @Schema(description = "适用环节")
    private String applyType;

    @Schema(description = "启用状态")
    private Integer status;

    @Schema(description = "审核状态")
    private Integer auditStatus;
}
