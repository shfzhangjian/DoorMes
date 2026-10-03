package cn.iocoder.yudao.module.mes.dal.dataobject.hc.route;

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
 * 工艺路线 DO
 */
@TableName("mes_md_route")
@KeySequence("mes_md_route_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcRouteDO extends BaseDO {

    /** 路线编码 */
    private String routeCode;

    /** 路线名称 */
    private String routeName;

    /** 适用范围 */
    private String applicableScope;

    /** 适用物料ID */
    private Long productMaterialId;

    /** 适用物料编码 */
    private String productMaterialCode;

    /** 适用层级 */
    private String productLevel;

    /** 版本号 */
    private String versionNo;

    /** 路线类型 */
    private String routeType;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
