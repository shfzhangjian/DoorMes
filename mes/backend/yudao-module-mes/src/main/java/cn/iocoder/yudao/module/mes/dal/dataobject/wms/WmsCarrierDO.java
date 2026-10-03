// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/wms/WmsCarrierDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.wms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 载具与周转箱台账 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_wms_carrier")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsCarrierDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 载具/托盘条码
     */
    private String carrierCode;

    /**
     * 载具类型(PALLET托盘, BOX周转箱)
     */
    private String carrierType;

    /**
     * 状态(EMPTY空闲, OCCUPIED被占用, MAINTENANCE维修中)
     */
    private String status;

    /**
     * 当前物理位置(工位/库区)
     */
    private String currentLocation;

}
