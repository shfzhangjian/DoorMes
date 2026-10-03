package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_sample_evaluation_project_user")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSampleEvaluationProjectUserDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String deptCode;
    private String deptName;
    private Long userId;
    private String userName;
    private Boolean canInitiate;
    private Boolean canAssign;
    private Integer sortNo;
    private String remark;
    private Long tenantId;

}
