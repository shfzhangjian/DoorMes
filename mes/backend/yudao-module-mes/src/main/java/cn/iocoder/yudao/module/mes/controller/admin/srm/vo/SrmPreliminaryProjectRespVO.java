package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmPreliminaryProjectRespVO {

    private Long id;
    private String projectCode;
    private String projectName;
    private Long currentTemplateId;
    private Long currentTemplateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionNoSnapshot;
    private String status;
    private String remark;
    private Integer version;
    private List<ScorerConfigItem> scorerItems;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class ScorerConfigItem {
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
    }

}
