package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceDeviceLedgerPageReqVO extends PageParam {

    @Schema(description = "设备编号")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类ID集合")
    private List<Long> categoryIds;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "部门")
    private String usingDepartment;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;

}
