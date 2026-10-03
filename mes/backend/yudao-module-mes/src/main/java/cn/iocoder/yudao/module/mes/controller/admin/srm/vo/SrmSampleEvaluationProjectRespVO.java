package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmSampleEvaluationProjectRespVO {

    private Long id;
    private String projectCode;
    private String projectName;
    private String status;
    private String remark;
    private Integer version;
    private List<UserConfig> users;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class UserConfig {
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
    }

    @Data
    public static class AssignableInspector {
        private Long userId;
        private String userName;
        private Long deptId;
        private String deptName;
        private Boolean recommended;
    }

    @Data
    public static class AssignableInspectors {
        private Long projectId;
        private String projectCode;
        private String projectName;
        private Long recommendedUserId;
        private String recommendedUserName;
        private List<AssignableInspector> users;
    }

}
