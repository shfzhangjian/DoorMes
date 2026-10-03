package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSampleEvaluationProjectService;
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

@Tag(name = "管理后台 - SRM样品评价项目配置")
@RestController
@RequestMapping("/mes/srm/sample-evaluation-project")
@Validated
public class SrmSampleEvaluationProjectController {

    @Resource
    private SrmSampleEvaluationProjectService projectService;

    @PostMapping("/create")
    @Operation(summary = "创建样品评价项目")
    public CommonResult<Long> create(@Valid @RequestBody SrmSampleEvaluationProjectSaveReqVO reqVO) {
        return success(projectService.createProject(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样品评价项目")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmSampleEvaluationProjectSaveReqVO reqVO) {
        projectService.updateProject(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样品评价项目")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        projectService.deleteProject(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样品评价项目详情")
    public CommonResult<SrmSampleEvaluationProjectRespVO> get(@RequestParam("id") Long id) {
        return success(projectService.getProject(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样品评价项目分页")
    public CommonResult<PageResult<SrmSampleEvaluationProjectRespVO>> page(
            @Valid SrmSampleEvaluationProjectPageReqVO reqVO) {
        return success(projectService.getProjectPage(reqVO));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的样品评价项目列表")
    public CommonResult<List<SrmSampleEvaluationProjectRespVO>> enabledList() {
        return success(projectService.getEnabledProjectList());
    }

}
