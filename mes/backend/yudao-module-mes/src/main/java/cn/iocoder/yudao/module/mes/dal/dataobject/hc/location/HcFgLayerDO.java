package cn.iocoder.yudao.module.mes.dal.dataobject.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 包装成品库货架层主数据。
 */
@TableName("mes_inv_fg_layer")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcFgLayerDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long rackId;
    /** 固定物理层号，例如 1，对应库位编码中的 L1。 */
    private Integer layerNo;
    private String layerName;
    private String status;
    private Integer sortNo;
    private String remark;
}
