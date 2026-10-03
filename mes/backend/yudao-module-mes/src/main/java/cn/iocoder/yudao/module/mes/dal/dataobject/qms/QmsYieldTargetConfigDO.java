package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 产品型号-工序目标合格配置。
 */
@TableName("mes_qms_yield_target_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsYieldTargetConfigDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String modelCode;
    private String processCode;
    private String processName;
    private String measureUnit;
    private String targetType;
    private Integer segmentCount;
    private BigDecimal targetQualifiedQty;
    private Integer status;
    private Integer sort;
    private String remark;
    private Long tenantId;
}
