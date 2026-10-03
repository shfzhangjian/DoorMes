package cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 打印程序版本分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcPrintAgentPackagePageReqVO extends PageParam {

    @Schema(description = "程序编码")
    private String packageCode;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "附件名称")
    private String packageName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否当前版本")
    private Boolean currentFlag;

}
