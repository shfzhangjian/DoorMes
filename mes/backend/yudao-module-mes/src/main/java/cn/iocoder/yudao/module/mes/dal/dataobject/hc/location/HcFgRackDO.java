package cn.iocoder.yudao.module.mes.dal.dataobject.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 包装成品库货架主数据。
 */
@TableName("mes_inv_fg_rack")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcFgRackDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    /** 所属包装成品仓库 ID。 */
    private Long warehouseId;
    /** 固定物理编码，例如 1；创建后不作为改名字段使用。 */
    private Integer rackNo;
    /** 货架展示名称，例如“成品白垫 A 架”。 */
    private String rackName;
    private String warehouseCode;
    private String warehouseName;
    private String status;
    private Integer sortNo;
    private String remark;
}
