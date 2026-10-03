package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 设备年度保养计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ResourceDeviceMaintPlanPageReqVO extends PageParam {

    private Integer planYear;
    private String deviceCode;
    private String deviceName;
    private String maintType;
    private Integer monthNo;
    private Integer weekNo;
    private Boolean published;
    private String status;

}
