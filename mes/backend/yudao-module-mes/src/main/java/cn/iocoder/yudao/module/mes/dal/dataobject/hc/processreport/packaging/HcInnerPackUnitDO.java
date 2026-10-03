package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_inner_pack_unit")
@KeySequence("mes_sfc_inner_pack_unit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcInnerPackUnitDO extends BaseDO {

    @TableId
    private Long id;

    private String innerUnitNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Integer packageSpec;
    private Integer targetQty;
    private Integer currentQty;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private String productSize;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private String locationName;
    private LocalDate packageDate;
    private String labelNo;
    private String unitStatus;
    private Integer printCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;
    private String reviewerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;
    private String lockUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockTime;
    private String inboundUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inboundTime;
    private Boolean backfillFlag;
    private String backfillReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime actualWorkTime;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
