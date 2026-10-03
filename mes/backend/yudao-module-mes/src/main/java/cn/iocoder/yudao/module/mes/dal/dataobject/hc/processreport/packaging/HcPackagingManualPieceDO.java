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

@TableName("mes_sfc_packaging_manual_piece")
@KeySequence("mes_sfc_packaging_manual_piece_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackagingManualPieceDO extends BaseDO {

    @TableId
    private Long id;

    private String sliceBatchNo;
    private String segmentBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String productSize;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private String inspectionResult;
    private String coaInspectionResult;
    private String recordStatus;
    private Long innerUnitId;
    private String innerUnitNo;
    private String printStatus;
    private Integer printCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;
    private String backfillReason;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String deleteReason;
    private String deleteUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deleteTime;
    private String remark;
    private Long tenantId;
}
