package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_8d_team_member")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Qms8dTeamMemberDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;
    private String memberRole;
    private Long deptId;
    private String deptName;
    private Long userId;
    private String userName;
    private String responsibility;
    private Integer sort;
    private Long tenantId;
}
