package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_evaluation_template")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmEvaluationTemplateDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateCode;
    private String templateName;
    private String sceneType;
    private String materialType;
    private Long currentVersionId;
    private String currentVersionNo;
    private String status;
    private String remark;
    @Version
    private Integer version;
    private Long tenantId;

}
