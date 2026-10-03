package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ResourceDeviceExceptionPageReqVO extends PageParam {

    private String orderNo;
    private String deviceCode;
    private String deviceName;
    private String exceptionLevel;
    private String status;
    private String tabType;
    private Long currentUserId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;

}
