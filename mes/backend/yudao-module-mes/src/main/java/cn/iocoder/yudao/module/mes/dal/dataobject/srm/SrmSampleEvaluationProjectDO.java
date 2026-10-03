package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_sample_evaluation_project")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSampleEvaluationProjectDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectCode;
    private String projectName;
    private String status;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
