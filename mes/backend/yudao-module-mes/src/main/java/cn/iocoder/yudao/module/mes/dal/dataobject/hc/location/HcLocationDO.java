package cn.iocoder.yudao.module.mes.dal.dataobject.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 库位 DO
 */
@TableName("mes_inv_location")
@KeySequence("mes_inv_location_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcLocationDO extends BaseDO {

    /** 库位编码 */
    private String locationCode;

    /** 库位名称 */
    private String locationName;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 库位类型 */
    private String locationType;

    /** 包装成品库质量用途（QUALIFIED/QUARANTINE/UNASSIGNED）；仅 biz_scene=PACKAGE_FG 使用。 */
    private String qualityScope;

    /** 业务场景 */
    private String bizScene;

    /** 位置描述 */
    private String positionDesc;

    /** 包装成品仓库主数据 ID；仅 biz_scene=PACKAGE_FG 使用。 */
    private Long warehouseId;

    /** 包装成品库货架主数据 ID；仅 biz_scene=PACKAGE_FG 使用。 */
    private Long rackId;

    /** 包装成品库层主数据 ID；仅 biz_scene=PACKAGE_FG 使用。 */
    private Long layerId;

    /** 区域号；仅 biz_scene=PACKAGE_FG 使用。 */
    private Integer areaNo;

    /** 库容量 */
    private Integer capacityQty;

    /** 已占容量 */
    private Integer occupiedQty;

    /** 九宫格序号 */
    private Integer gridNo;

    /** 二维码内容 */
    private String qrCode;

    /** 是否允许混批 */
    private Boolean mixBatchFlag;

    /** 是否允许混型号 */
    private Boolean mixModelFlag;

    /** 状态 */
    private String status;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
