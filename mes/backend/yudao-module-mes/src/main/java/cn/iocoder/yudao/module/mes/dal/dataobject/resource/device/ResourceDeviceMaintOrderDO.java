package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_maint_order")
@KeySequence("mes_resource_device_maint_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceMaintOrderDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private String planPeriod;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long categoryId;
    private String categoryName;
    private Long standardId;
    private String standardCode;
    private String standardName;
    private String frequency;
    private String maintType;
    private String taskDesc;
    private String executor;
    private LocalDate planDate;
    private LocalDate dueDate;
    private LocalDateTime planTime;
    private LocalDateTime actualDate;
    private String executeRemark;
    private String confirmer;
    private LocalDateTime confirmTime;
    private String photos;
    private String status;
    private Long exceptionId;
    private String exceptionNo;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
