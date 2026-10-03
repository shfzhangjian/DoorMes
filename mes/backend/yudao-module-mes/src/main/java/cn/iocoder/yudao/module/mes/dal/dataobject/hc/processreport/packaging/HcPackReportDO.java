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

@TableName("mes_sfc_pack_report")
@KeySequence("mes_sfc_pack_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private String productionBatchNo;
    private LocalDate packageDate;
    private Integer innerUnitCount;
    private Integer innerPieceCount;
    private Integer outerBoxCount;
    private Integer outerPieceCount;
    private Integer inboundPieceCount;
    private String reportStatus;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String confirmerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
