package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备台账新增/修改 Request VO")
@Data
public class ResourceDeviceLedgerSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "设备编号不能为空")
    @Size(max = 64, message = "设备编号不能超过64个字符")
    private String deviceCode;

    @Schema(description = "设备名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "设备名称不能为空")
    @Size(max = 128, message = "设备名称不能超过128个字符")
    private String deviceName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "规格型号")
    private String model;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "制造厂商")
    private String manufacturer;

    @Schema(description = "长")
    private String deviceLength;

    @Schema(description = "宽")
    private String deviceWidth;

    @Schema(description = "高")
    private String deviceHeight;

    @Schema(description = "资产编号")
    private String assetNo;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "责任人")
    private String responsiblePerson;

    @Schema(description = "出厂日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate factoryDate;

    @Schema(description = "设备购买时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @Schema(description = "设备安装时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate installDate;

    @Schema(description = "设备使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate useDate;

    @Schema(description = "投用日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate commissioningDate;

    @Schema(description = "状态(1正常 0闲置 2维修保养中 3报废)")
    private Integer status;

    @Schema(description = "保养状态")
    private String maintStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁")
    private Integer version;

    @Valid
    private List<DevicePart> parts;

    @Valid
    private List<DeviceParam> params;

    @Data
    public static class DevicePart {
        private Long id;
        private Long deviceId;
        private String partCode;
        private String partName;
        private String spec;
        private BigDecimal quantity;
        private Integer replaceCycle;
        private String remark;
        private Integer sort;
    }

    @Data
    public static class DeviceParam {
        private Long id;
        private Long deviceId;
        private String paramName;
        private String paramValue;
        private String unit;
        private String remark;
        private Integer sort;
    }

}
