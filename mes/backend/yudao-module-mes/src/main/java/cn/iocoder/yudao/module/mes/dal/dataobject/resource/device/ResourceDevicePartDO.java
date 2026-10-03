package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_part")
@KeySequence("mes_resource_device_part_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDevicePartDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;
    private String partCode;
    private String partName;
    private String spec;
    private BigDecimal quantity;
    private Integer replaceCycle;
    private String remark;
    private Integer sort;
    private Long tenantId;

}
