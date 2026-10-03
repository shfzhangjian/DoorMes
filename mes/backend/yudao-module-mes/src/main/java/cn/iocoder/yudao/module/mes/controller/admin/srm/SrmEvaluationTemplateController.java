package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmEvaluationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM供应商评估模板")
@RestController
@RequestMapping("/mes/srm/evaluation-template")
@Validated
public class SrmEvaluationTemplateController {

    @Resource
    private SrmEvaluationTemplateService templateService;

    @PostMapping("/create")
    @Operation(summary = "创建评估模板及首版草稿")
    public CommonResult<Long> create(@Valid @RequestBody SrmEvaluationTemplateSaveReqVO reqVO) {
        return success(templateService.createTemplate(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新评估模板草稿")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmEvaluationTemplateSaveReqVO reqVO) {
        templateService.updateTemplate(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得评估模板、版本、指标和日志")
    public CommonResult<SrmEvaluationTemplateRespVO> get(@RequestParam("id") Long id,
                                                         @RequestParam(value = "versionId", required = false)
                                                         Long versionId) {
        return success(templateService.getTemplate(id, versionId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得评估模板分页")
    public CommonResult<PageResult<SrmEvaluationTemplateRespVO>> page(
            @Valid SrmEvaluationTemplatePageReqVO reqVO) {
        return success(templateService.getTemplatePage(reqVO));
    }

    @GetMapping("/published-list")
    @Operation(summary = "获得可引用的已发布模板")
    public CommonResult<List<SrmEvaluationTemplateRespVO>> publishedList(
            @RequestParam(value = "sceneType", required = false) String sceneType) {
        return success(templateService.getPublishedTemplateList(sceneType));
    }

    @PutMapping("/submit-audit")
    @Operation(summary = "提交模板版本审核")
    public CommonResult<Boolean> submitAudit(@Valid @RequestBody SrmEvaluationTemplateActionReqVO reqVO) {
        templateService.submitAudit(reqVO);
        return success(true);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核模板版本")
    public CommonResult<Boolean> audit(@Valid @RequestBody SrmEvaluationTemplateActionReqVO reqVO) {
        templateService.audit(reqVO);
        return success(true);
    }

    @PutMapping("/publish")
    @Operation(summary = "发布模板版本")
    public CommonResult<Boolean> publish(@Valid @RequestBody SrmEvaluationTemplateActionReqVO reqVO) {
        templateService.publish(reqVO);
        return success(true);
    }

    @PostMapping("/upgrade")
    @Operation(summary = "克隆已发布版本生成升级草稿")
    public CommonResult<Long> upgrade(@Valid @RequestBody SrmEvaluationTemplateActionReqVO reqVO) {
        return success(templateService.upgrade(reqVO));
    }

}
