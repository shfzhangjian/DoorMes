package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SrmSampleEvaluationProjectSaveReqVO {

    private Long id;
    @NotEmpty(message = "项目编码不能为空")
    private String projectCode;
    @NotEmpty(message = "项目名称不能为空")
    private String projectName;
    private String status;
    private String remark;
    private Integer version;
    @Valid
    private List<UserConfig> users;

    @Data
    public static class UserConfig {

        private Long id;
        private String deptCode;
        private String deptName;
        private Long userId;
        private String userName;
        private Boolean canInitiate;
        private Boolean canAssign;
        private Integer sortNo;
        private String remark;

    }

}
