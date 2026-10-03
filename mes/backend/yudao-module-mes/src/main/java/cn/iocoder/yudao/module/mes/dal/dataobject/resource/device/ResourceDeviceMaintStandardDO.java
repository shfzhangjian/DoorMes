package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_maint_standard")
@KeySequence("mes_resource_device_maint_standard_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceMaintStandardDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String deviceType;
    private String frequency;
    private String maintType;
    private Integer estimatedMinutes;
    private Integer status;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
