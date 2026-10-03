package cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 货主分页 Request VO")
@Data
public class HcOwnerPageReqVO extends PageParam {

    @Schema(description = "货主编码")
    private String ownerCode;

    @Schema(description = "货主名称")
    private String ownerName;

    @Schema(description = "货主类型")
    private String ownerType;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "状态")
    private String status;

}