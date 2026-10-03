package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 压槽备件状态 Response VO")
@Data
public class HcPressSlotSpareRespVO {

    private Long id;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String spareType;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private BigDecimal onlineQuantity;
    private BigDecimal availableQuantity;
    private Integer useCount;
    private Integer limitCount;
    private Integer limitDays;
    private Integer warningFlag;
    private String status;
    private Long lastOperatorId;
    private String lastOperatorName;
    private String lastReplaceReason;
    private String lastCleanRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReplaceTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastCleanTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastEventTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
