package cn.iocoder.yudao.module.mes.dal.dataobject.hc.team;

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
 * 班组 DO
 */
@TableName("mes_md_team")
@KeySequence("mes_md_team_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcTeamDO extends BaseDO {

    /** 班组编码 */
    private String teamCode;

    /** 班组名称 */
    private String teamName;

    /** 默认工作中心ID */
    private Long workCenterId;

    /** 默认工作中心编码 */
    private String workCenterCode;

    /** 班组长用户ID */
    private Long leaderUserId;

    /** 班组长姓名 */
    private String leaderName;

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