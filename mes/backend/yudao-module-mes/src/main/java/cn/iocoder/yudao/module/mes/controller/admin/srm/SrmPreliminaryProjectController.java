package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectScorerConfigReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmPreliminaryProjectService;
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

@Tag(name = "管理后台 - SRM初评项目配置")
@RestController
@RequestMapping("/mes/srm/preliminary-project")
@Validated
public class SrmPreliminaryProjectController {

    @Resource
    private SrmPreliminaryProjectService projectService;

    @PostMapping("/create")
    @Operation(summary = "创建初评项目")
    public CommonResult<Long> create(@Valid @RequestBody SrmPreliminaryProjectSaveReqVO reqVO) {
        return success(projectService.createProject(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新初评项目")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmPreliminaryProjectSaveReqVO reqVO) {
        projectService.updateProject(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除初评项目")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        projectService.deleteProject(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得初评项目详情")
    public CommonResult<SrmPreliminaryProjectRespVO> get(@RequestParam("id") Long id) {
        return success(projectService.getProject(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得初评项目分页")
    public CommonResult<PageResult<SrmPreliminaryProjectRespVO>> page(@Valid SrmPreliminaryProjectPageReqVO reqVO) {
        return success(projectService.getProjectPage(reqVO));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的初评项目列表")
    public CommonResult<List<SrmPreliminaryProjectRespVO>> enabledList() {
        return success(projectService.getEnabledProjectList());
    }

    @GetMapping("/scorer-config")
    @Operation(summary = "获得项目模板指标评分人配置")
    public CommonResult<SrmPreliminaryProjectRespVO> scorerConfig(@RequestParam("projectId") Long projectId,
                                                                  @RequestParam("templateVersionId") Long templateVersionId) {
        return success(projectService.getScorerConfig(projectId, templateVersionId));
    }

    @PutMapping("/scorer-config")
    @Operation(summary = "保存项目模板指标评分人配置")
    public CommonResult<Boolean> saveScorerConfig(
            @Valid @RequestBody SrmPreliminaryProjectScorerConfigReqVO reqVO) {
        projectService.saveScorerConfig(reqVO);
        return success(true);
    }

}
