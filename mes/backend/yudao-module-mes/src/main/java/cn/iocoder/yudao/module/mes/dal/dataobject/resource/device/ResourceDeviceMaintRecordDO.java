package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_maint_record")
@KeySequence("mes_resource_device_maint_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceMaintRecordDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;
    private Long orderId;
    private String taskNo;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long categoryId;
    private String categoryName;
    private Long standardId;
    private String standardName;
    private String maintType;
    private String frequency;
    private String executor;
    private LocalDate dueDate;
    private LocalDateTime planTime;
    private LocalDateTime actualTime;
    private String confirmer;
    private LocalDateTime confirmTime;
    private String photos;
    private String resultStatus;
    private Long exceptionId;
    private String exceptionNo;
    private String remark;
    private Long tenantId;

}
