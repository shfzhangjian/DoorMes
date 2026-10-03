package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备保养工单新增/修改 Request VO")
@Data
public class ResourceDeviceMaintOrderSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    private String taskNo;
    private String planPeriod;
    @NotNull(message = "设备不能为空")
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long standardId;
    private String standardName;
    private String frequency;
    private String maintType;
    private String taskDesc;
    private String executor;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime planTime;

    private String executeRemark;
    private String status;
    private String remark;
    private Integer version;

    @Valid
    private List<OrderItem> items;

    @Valid
    private List<OrderPart> parts;

    @Data
    public static class OrderItem {
        private Long id;
        private Long taskId;
        private Long standardItemId;
        private String itemName;
        private String method;
        private String requirement;
        private String result;
        private String remark;
        private Integer sort;
    }

    @Data
    public static class OrderPart {
        private Long id;
        private Long taskId;
        private String partCode;
        private String partName;
        private BigDecimal quantity;
        private String unit;
        private String remark;
        private Integer sort;
    }

}
