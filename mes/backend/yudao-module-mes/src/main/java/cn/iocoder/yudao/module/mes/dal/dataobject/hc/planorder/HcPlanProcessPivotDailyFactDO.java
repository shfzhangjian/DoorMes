package cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 生产进度（日结版）物化结果。
 */
@TableName("mes_sfc_process_pivot_daily_fact")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcPlanProcessPivotDailyFactDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;
    private Long planId;
    private String planNo;
    private String planStatus;
    private String planMode;
    private String sourceType;
    private String salesOrderNo;
    private String salesOrderErpNo;
    private LocalDate planDate;
    private LocalDate productionStartDate;
    private LocalDate productionEndDate;
    private String materialCode;
    private String materialName;
    private String motherMaterialCode;
    private String motherMaterialName;
    private String modelCode;
    private String modelName;
    private String motherModelCode;
    private String motherModelName;
    private String prodType;
    private String categoryCode;
    private String recipeCode;
    private String routeCode;
    private String routeName;
    private String sizeSpec;
    private String sizeName;
    private BigDecimal targetQty;
    private BigDecimal netPlanQty;
    private String targetUom;
    private String batchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String motherRollBatchNo;
    private String segmentBatchNo;
    private String actualModelCode;
    private String actualSizeSpec;
    private Boolean postProcessFlag;
    private BigDecimal totalDefectQty;
    private LocalDateTime latestReportTime;
    private LocalDateTime sourceCreateTime;
    private LocalDateTime sourceUpdateTime;
    private String pivotKey;
    private String pivotKeyHash;
    private String sourceContentHash;
    private String pivotJson;
    private String syncBatchNo;
    private LocalDateTime syncTime;
    private Long tenantId;
}
