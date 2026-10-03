package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_preliminary_project_scorer")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPreliminaryProjectScorerDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long templateId;
    private Long templateVersionId;
    private Long templateItemId;
    private String groupCodeSnapshot;
    private String groupNameSnapshot;
    private Integer groupSort;
    private String indicatorCodeSnapshot;
    private String indicatorNameSnapshot;
    private Integer indicatorSort;
    private String defaultDeptNames;
    private String scorerCandidateUserIds;
    private String scorerCandidateUserNames;
    private Long scorerUserId;
    private String scorerUserName;
    private Long tenantId;

}
