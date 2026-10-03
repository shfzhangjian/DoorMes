package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_evaluation_template_log")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmEvaluationTemplateLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long versionId;
    private String action;
    private String actionDescription;
    private Long operatorId;
    private String operatorName;
    private String beforeJson;
    private String afterJson;
    private Long tenantId;

}
