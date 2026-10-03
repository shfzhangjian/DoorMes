package cn.iocoder.yudao.module.mes.dal.dataobject.hc.route;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 工艺路线 DO
 */
@TableName("mes_md_route_operation")
@KeySequence("mes_md_route_operation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcRouteOperationDO extends BaseDO {

    /** 工艺路线ID */
    private Long routeId;

    /** 路线编码 */
    private String routeCode;

    /** 工序编码 */
    private String operationCode;

    /** 工序名称 */
    private String operationName;

    /** 工序顺序号 */
    private Integer seqNo;

    /** 默认工作中心ID */
    private Long workCenterId;

    /** 默认工作中心编码 */
    private String workCenterCode;

    /** 默认工作中心名称 */
    private String workCenterName;

    /** 是否必须报工 */
    private Boolean reportRequired;

    /** 转换率/收得率 */
    private BigDecimal conversionRate;

    /** 产出单位ID */
    private Long outputUnitId;

    /** 产出单位符号 */
    private String outputUnitCode;

    /** 产出单位名称 */
    private String outputUnitName;

    /** 产出计量单位 */
    private String outputUom;

    /** 是否必须送检 */
    private Boolean qcRequired;

    /** 批次拆分方式 */
    private String batchSplitMode;

    /** 参数模板JSON */
    private String paramTemplateJson;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
