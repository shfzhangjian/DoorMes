package cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 分切压槽不合格品货架主数据。 */
@TableName("mes_inv_ng_rack")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcNgInventoryRackDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long warehouseId;
    private Integer rackNo;
    /** 库位展示编码第二段，例如 FQ1、1、01。 */
    private String rackCode;
    private String rackName;
    private String storagePurpose;
    private String status;
    private Integer sortNo;
    private String remark;
}
