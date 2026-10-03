package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 成品胶板对照表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcFinishedGlueBoardMapPageReqVO extends PageParam {

    @Schema(description = "产品型号")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "成品规格")
    private String productSpec;

    @Schema(description = "胶板关键字")
    private String glueBoardKeyword;

    @Schema(description = "状态")
    private String status;

}
