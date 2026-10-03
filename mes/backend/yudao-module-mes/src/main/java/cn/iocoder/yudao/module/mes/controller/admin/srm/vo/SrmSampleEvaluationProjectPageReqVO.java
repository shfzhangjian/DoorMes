package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSampleEvaluationProjectPageReqVO extends PageParam {

    private String projectCode;
    private String projectName;
    private String status;

}
