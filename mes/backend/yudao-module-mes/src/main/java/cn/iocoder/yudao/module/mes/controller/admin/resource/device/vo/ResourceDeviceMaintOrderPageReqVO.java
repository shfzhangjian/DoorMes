package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备保养工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceDeviceMaintOrderPageReqVO extends PageParam {

    private String taskNo;
    private String deviceCode;
    private String deviceName;
    private String maintType;
    private String status;
    private String tabType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] planDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] actualDate;

}
