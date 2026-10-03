package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备台账 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ResourceDeviceLedgerRespVO {

    private Long id;
    @ExcelProperty("序号")
    private Integer rowNo;
    @ExcelProperty("设备编号")
    private String deviceCode;
    @ExcelProperty("部门")
    private String usingDepartment;
    @ExcelProperty("设备类别")
    private String deviceType;
    private Long categoryId;
    @ExcelProperty("设备分类（二级分类）")
    private String categoryName;
    @ExcelProperty("设备名称")
    private String deviceName;
    @ExcelProperty("规格型号")
    private String model;
    private String specification;
    @ExcelProperty("制造商名称")
    private String manufacturer;
    @ExcelProperty("长")
    private String deviceLength;
    @ExcelProperty("宽")
    private String deviceWidth;
    @ExcelProperty("高")
    private String deviceHeight;
    private String assetNo;
    @ExcelProperty("具体位置")
    private String location;
    private String responsiblePerson;
    @ExcelProperty("设备购买时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate purchaseDate;
    @ExcelProperty("设备安装时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate installDate;
    @ExcelProperty("设备使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate useDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate factoryDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate commissioningDate;
    private Integer status;
    private String maintStatus;
    @ExcelProperty("备注")
    private String remark;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private List<DevicePart> parts;
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
