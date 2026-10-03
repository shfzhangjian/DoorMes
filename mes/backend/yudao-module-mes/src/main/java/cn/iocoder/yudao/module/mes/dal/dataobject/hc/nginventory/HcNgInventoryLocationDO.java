package cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 分切、压槽不合格品库的独立精确库位。 */
@TableName("mes_inv_ng_location")
@KeySequence("mes_inv_ng_location_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcNgInventoryLocationDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    /** 系统内部唯一库位键；创建后不可修改，不在页面展示。 */
    private String locationKey;
    /** 现场展示编码，可按业务需要维护。 */
    private String locationCode;
    private String locationName;
    private String warehouseCode;
    private String warehouseName;
    private Long warehouseId;
    private Long rackId;
    private String rackNo;
    /** 货架内库位编号。 */
    private Integer locationNo;
    /** 库位用途：SLITTING_NG/PRESS_SLOT_NG/FREEZE。 */
    private String storagePurpose;
    private Integer capacityQty;
    private Integer occupiedQty;
    private Integer gridNo;
    private String status;
}
