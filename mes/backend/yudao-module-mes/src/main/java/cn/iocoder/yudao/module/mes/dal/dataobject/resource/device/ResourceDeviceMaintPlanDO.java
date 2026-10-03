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

@TableName("mes_resource_device_maint_plan")
@KeySequence("mes_resource_device_maint_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceMaintPlanDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer planYear;
    private String planPeriod;
    private Integer monthNo;
    private Integer weekNo;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDate planDate;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long categoryId;
    private String categoryName;
    private Long standardId;
    private String standardCode;
    private String standardName;
    private Long standardItemId;
    private String itemGroup;
    private String itemName;
    private String method;
    private String requirement;
    private String frequency;
    private String maintType;
    private String sourceRule;
    private Boolean published;
    private Long generatedOrderId;
    private String generatedTaskNo;
    private String executeStatus;
    private LocalDateTime actualDate;
    private String executor;
    private String executeRemark;
    private String confirmer;
    private LocalDateTime confirmTime;
    private String status;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
