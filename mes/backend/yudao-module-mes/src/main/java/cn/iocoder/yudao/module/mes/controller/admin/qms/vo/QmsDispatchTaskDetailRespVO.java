package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务 ERP 详情 Response VO")
@Data
public class QmsDispatchTaskDetailRespVO {

    private QmsDispatchTaskRespVO task;
    private List<QmsDispatchTaskCandidateItemRespVO> items;
    private List<QmsDispatchTaskRoundRespVO> rounds;
    private Boolean editable;
    private String activeTaskKey;
    private String activeTaskName;
    private Boolean actionable;
    private List<QmsDispatchTaskSampleResultRespVO> samples;
}
