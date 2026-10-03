package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备保养标准 Response VO")
@Data
public class ResourceDeviceMaintStandardRespVO {

    private Long id;
    @ExcelProperty("标准编号")
    private String code;
    @ExcelProperty("标准名称")
    private String name;
    private Long categoryId;
    @ExcelProperty("设备分类")
    private String categoryName;
    @ExcelProperty("设备类型")
    private String deviceType;
    @ExcelProperty("执行频率")
    private String frequency;
    @ExcelProperty("保养类型")
    private String maintType;
    private Integer estimatedMinutes;
    @ExcelProperty("状态")
    private Integer status;
    private String remark;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    private List<StandardItem> items;

    @Data
    public static class StandardItem {
        private Long id;
        private Long standardId;
        private String itemGroup;
        private String itemName;
        private String method;
        private String requirement;
        private String frequency;
        private String tool;
        private String resultType;
        private Boolean requiredFlag;
        private Integer sort;
    }

}
