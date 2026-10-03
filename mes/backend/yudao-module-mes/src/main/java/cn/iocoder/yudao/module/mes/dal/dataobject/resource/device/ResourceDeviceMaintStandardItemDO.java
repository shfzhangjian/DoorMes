package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_maint_standard_item")
@KeySequence("mes_resource_device_maint_standard_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceMaintStandardItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long standardId;
    private String itemGroup;
    private String itemName;
    private String method;
    private String requirement;
    private String frequency;
    private String tool;
    private String resultType;
    private Boolean requiredFlag;
    private Integer sort;
    private Long tenantId;

}
