package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEntryRuleTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEntryRuleTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsEntryRuleTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 检验标准录入规则模板")
@RestController
@RequestMapping("/mes/quality/base/standard/entry-rule-template")
@Validated
public class QmsEntryRuleTemplateController {

    private static final String QUALITY_STANDARD_LIST_PERMISSIONS = "@ss.hasAnyPermissions("
            + "'mes:quality-standard:list', "
            + "'mes:quality-standard:iqc:list', "
            + "'mes:quality-standard:fai:list', "
            + "'mes:quality-standard:glue-board-fai:list', "
            + "'mes:quality-standard:ipqc:list', "
            + "'mes:quality-standard:fqc:list', "
            + "'mes:quality-standard:oqc:list')";
    private static final String QUALITY_STANDARD_EDIT_PERMISSIONS = "@ss.hasAnyPermissions("
            + "'mes:quality-standard:create', "
            + "'mes:quality-standard:update', "
            + "'mes:quality-standard:iqc:create', "
            + "'mes:quality-standard:iqc:update', "
            + "'mes:quality-standard:fai:create', "
            + "'mes:quality-standard:fai:update', "
            + "'mes:quality-standard:glue-board-fai:create', "
            + "'mes:quality-standard:glue-board-fai:update', "
            + "'mes:quality-standard:ipqc:create', "
            + "'mes:quality-standard:ipqc:update', "
            + "'mes:quality-standard:fqc:create', "
            + "'mes:quality-standard:fqc:update', "
            + "'mes:quality-standard:oqc:create', "
            + "'mes:quality-standard:oqc:update')";

    @Resource
    private QmsEntryRuleTemplateService qmsEntryRuleTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建录入规则模板")
    public CommonResult<Long> createEntryRuleTemplate(@Valid @RequestBody QmsEntryRuleTemplateSaveReqVO createReqVO) {
        return success(qmsEntryRuleTemplateService.createEntryRuleTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新录入规则模板")
    public CommonResult<Boolean> updateEntryRuleTemplate(@Valid @RequestBody QmsEntryRuleTemplateSaveReqVO updateReqVO) {
        qmsEntryRuleTemplateService.updateEntryRuleTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除录入规则模板")
    public CommonResult<Boolean> deleteEntryRuleTemplate(@RequestParam("id") Long id) {
        qmsEntryRuleTemplateService.deleteEntryRuleTemplate(id);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取录入规则模板列表")
    public CommonResult<List<QmsEntryRuleTemplateRespVO>> getEntryRuleTemplateList(
            @RequestParam(value = "itemType", required = false) String itemType) {
        return success(qmsEntryRuleTemplateService.getEntryRuleTemplateList(itemType));
    }
}
