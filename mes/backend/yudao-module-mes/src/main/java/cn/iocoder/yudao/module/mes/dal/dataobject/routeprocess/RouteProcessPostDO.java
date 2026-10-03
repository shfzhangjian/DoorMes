package cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess;

import lombok.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工序岗位定额 DO
 * 对应表: mes_route_process_post
 */
@TableName("mes_route_process_post")
@KeySequence("mes_route_process_post_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteProcessPostDO extends BaseDO {

    @TableId
    private Long id;

    /** 工艺路线工序ID */
    private Long routeProcessId;

    /** 岗位编码 */
    private String postCode;
    /** 岗位名称 */
    private String postName;

    /** 最低技能等级 */
    private String skillLevel;

    /** 标准工时(人*小时/单位产出) */
    private BigDecimal stdManHour;
    /** 最少作业人数 */
    private Integer minPerson;
}
