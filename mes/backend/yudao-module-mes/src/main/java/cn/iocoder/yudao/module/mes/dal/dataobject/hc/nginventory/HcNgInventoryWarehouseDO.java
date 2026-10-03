package cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 分切压槽不合格品仓库主数据。 */
@TableName("mes_inv_ng_warehouse")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcNgInventoryWarehouseDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String warehouseCode;
    private String warehouseName;
    private String padType;
    private String status;
    private Integer sortNo;
    private String remark;
}
