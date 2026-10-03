package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HcEquipmentConsumableStateImportExcelVO {

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

    @ExcelProperty("工序编码")
    private String processCode;

    @ExcelProperty("工序名称")
    private String processName;

    @ExcelProperty("耗材类型编码")
    private String consumableType;

    @ExcelProperty("耗材类型名称")
    private String consumableTypeName;

    @ExcelProperty("当前批号")
    private String batchNo;

    @ExcelProperty("累计米数(m)")
    private BigDecimal usedLength;

    @ExcelProperty("累计次数")
    private Integer useCount;

    @ExcelProperty("米数上限")
    private BigDecimal limitLength;

    @ExcelProperty("次数上限")
    private Integer limitCount;

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

    @ExcelProperty("最后操作人ID")
    private Long lastOperatorId;

    @ExcelProperty("最后操作人")
    private String lastOperatorName;

    @ExcelProperty("最后操作时间")
    private String lastEventTime;

    @ExcelProperty("备注")
    private String remark;
}
