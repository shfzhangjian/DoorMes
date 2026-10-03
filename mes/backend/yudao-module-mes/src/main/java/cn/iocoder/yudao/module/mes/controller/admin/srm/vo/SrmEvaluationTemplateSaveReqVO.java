package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 供应商评估模板新增/修改 Request VO")
@Data
public class SrmEvaluationTemplateSaveReqVO {

    private Long id;
    private Long versionId;

    @NotEmpty(message = "模板编码不能为空")
    private String templateCode;
    @NotEmpty(message = "模板名称不能为空")
    private String templateName;
    @NotEmpty(message = "适用场景不能为空")
    private String sceneType;
    private String materialType;
    private String templateStatus;
    private String templateRemark;

    @NotEmpty(message = "版本号不能为空")
    private String versionNo;
    @NotNull(message = "总分不能为空")
    @DecimalMin(value = "0.01", message = "总分必须大于0")
    private BigDecimal totalScore;
    @NotNull(message = "合格线不能为空")
    @DecimalMin(value = "0", message = "合格线不能小于0")
    private BigDecimal qualificationScore;
    private String changeSummary;
    private String versionRemark;
    private Integer version;

    @Valid
    @NotEmpty(message = "模板指标不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        @NotEmpty(message = "维度编码不能为空")
        private String groupCode;
        @NotEmpty(message = "维度名称不能为空")
        private String groupName;
        @NotNull(message = "维度排序不能为空")
        private Integer groupSort;
        @NotNull(message = "维度满分不能为空")
        private BigDecimal groupMaxScore;
        private String vetoOperator;
        private BigDecimal vetoScore;
        private String vetoResult;
        @NotEmpty(message = "指标编码不能为空")
        private String indicatorCode;
        @NotEmpty(message = "指标名称不能为空")
        private String indicatorName;
        @NotNull(message = "指标排序不能为空")
        private Integer indicatorSort;
        private String scoringRule;
        @NotNull(message = "指标满分不能为空")
        private BigDecimal maxScore;
        private String defaultDeptNames;
        private Long defaultScorerUserId;
        private String defaultScorerUserName;
        private String defaultScorerUserIds;
        private String defaultScorerUserNames;
        private Boolean attachmentRequired;
    }

}
