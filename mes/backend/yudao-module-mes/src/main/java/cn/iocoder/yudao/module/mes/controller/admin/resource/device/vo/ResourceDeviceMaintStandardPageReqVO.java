package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备保养标准分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceDeviceMaintStandardPageReqVO extends PageParam {

    private String code;
    private String name;
    private Long categoryId;
    private String deviceType;
    private String frequency;
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;

}
