// 完整路径: cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO
package cn.iocoder.yudao.module.mes.dal.dataobject.route;

import lombok.*;
import java.util.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * MES工艺路线主表 DO
 * 对应表: mes_route
 */
@TableName("mes_route")
@KeySequence("mes_route_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDO extends BaseDO {

    @TableId
    private Long id;
    /** 工艺路线编号 */
    private String code;
    /** 工艺路线名称 */
    private String name;

    /** 产品ID */
    private Long productId;
    // --- 冗余字段 (列表显示优化) ---
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;

    /** 版本号 */
    private String version;

    /** * 是否默认路线
     * 数据库字段: is_active
     */
    @TableField("is_active")
    private Boolean active;

    /** 备注 */
    private String remark;
    /** 状态 */
    private Integer status;

    private Long tenantId;
}
