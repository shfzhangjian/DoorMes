package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * NCR 评审会签单位默认办理人配置。
 */
@TableName("mes_qms_nc_review_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsNcReviewConfigDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String unitCode;
    private String unitName;
    private Long deptId;
    private String deptName;
    private String handlerUserIds;
    private String handlerUserNames;
    private Integer status;
    private Integer sort;
    private String remark;
    private String opinionTemplateJson;
    private Long tenantId;
}
