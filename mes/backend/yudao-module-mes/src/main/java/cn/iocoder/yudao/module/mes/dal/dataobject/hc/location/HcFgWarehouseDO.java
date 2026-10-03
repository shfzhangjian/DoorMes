package cn.iocoder.yudao.module.mes.dal.dataobject.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 包装成品仓库主数据。
 *
 * <p>仓库编码是包装成品库库位编码的第一段，例如 {@code BLACK-1-L1-2}。</p>
 */
@TableName("mes_inv_fg_warehouse")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcFgWarehouseDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String warehouseCode;
    private String warehouseName;
    private String status;
    private Integer sortNo;
    private String remark;
}
