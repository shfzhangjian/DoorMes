package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HcPressSlotSpareImportExcelVO {

    @ExcelProperty("设备ID")
    private Long equipmentId;

    @ExcelProperty("设备编码")
    private String equipmentCode;

    @ExcelProperty("设备名称")
    private String equipmentName;

    @ExcelProperty("工作中心ID")
    private Long workCenterId;

    @ExcelProperty("工作中心编码")
    private String workCenterCode;

    @ExcelProperty("工作中心名称")
    private String workCenterName;

    @ExcelProperty("备件类型编码")
    private String spareType;

    @ExcelProperty("备件类型名称")
    private String spareTypeName;

    @ExcelProperty("料号")
    private String materialCode;

    @ExcelProperty("名称")
    private String materialName;

    @ExcelProperty("批号/编码")
    private String batchNo;

    @ExcelProperty("在线数量")
    private BigDecimal onlineQuantity;

    @ExcelProperty("可用量")
    private BigDecimal availableQuantity;

    @ExcelProperty("累计片数")
    private Integer useCount;

    @ExcelProperty("片数上限")
    private Integer limitCount;

    @ExcelProperty("天数上限")
    private Integer limitDays;

    @ExcelProperty("预警标记")
    private Integer warningFlag;

    @ExcelProperty("预警状态")
    private String warningFlagName;

    @ExcelProperty("状态编码")
    private String status;

    @ExcelProperty("状态名称")
    private String statusName;

    @ExcelProperty("上次更换时间")
    private String lastReplaceTime;

    @ExcelProperty("上次计划号")
    private String lastReplacePlanNo;

    @ExcelProperty("上次更换原因")
    private String lastReplaceReason;

    @ExcelProperty("上次清洗时间")
    private String lastCleanTime;

    @ExcelProperty("上次清洗备注")
    private String lastCleanRemark;

    @ExcelProperty("最后操作人ID")
    private Long lastOperatorId;

    @ExcelProperty("最后操作人")
    private String lastOperatorName;

    @ExcelProperty("最后操作时间")
    private String lastEventTime;

    @ExcelProperty("备注")
    private String remark;
}
