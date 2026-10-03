package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_ledger")
@KeySequence("mes_resource_device_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceLedgerDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;
    private String deviceName;
    private Long categoryId;
    private String categoryName;
    private String deviceType;
    private String model;
    private String specification;
    private String manufacturer;
    private String deviceLength;
    private String deviceWidth;
    private String deviceHeight;
    private String assetNo;
    private String location;
    private String usingDepartment;
    private String responsiblePerson;
    private LocalDate factoryDate;
    private LocalDate purchaseDate;
    private LocalDate installDate;
    private LocalDate useDate;
    private LocalDate commissioningDate;
    private Integer status;
    private String maintStatus;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
