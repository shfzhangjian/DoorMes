package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SrmPreliminaryProjectScorerConfigReqVO {

    @NotNull(message = "请选择项目")
    private Long projectId;
    @NotNull(message = "请选择评估模板版本")
    private Long templateVersionId;
    @Valid
    @NotEmpty(message = "请维护模板指标评分人")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "模板指标不能为空")
        private Long templateItemId;
        private Long scorerUserId;
        private List<Long> scorerCandidateUserIds;
    }

}
