package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_supplier_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceSupplierConfigDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String configNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Long currentTemplateId;
    private Long currentTemplateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionNoSnapshot;
    private String status;
    private String remark;
    @Version
    private Integer version;
    private Long tenantId;

}
