package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskCodePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskCodePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.researchtask.HcResearchTaskDO;
import cn.iocoder.yudao.module.mes.service.hc.researchtask.HcResearchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - HC Research Task")
@RestController
@RequestMapping("/mes/hc/plan/research-task")
@Validated
public class HcResearchTaskController {

    @Resource
    private HcResearchTaskService researchTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建研发型号任务")
    public CommonResult<Long> createResearchTask(@Valid @RequestBody HcResearchTaskSaveReqVO createReqVO) {
        return success(researchTaskService.createResearchTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新研发型号任务")
    public CommonResult<Boolean> updateResearchTask(@Valid @RequestBody HcResearchTaskSaveReqVO updateReqVO) {
        researchTaskService.updateResearchTask(updateReqVO);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认研发型号任务")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> confirmResearchTask(@RequestParam("id") Long id) {
        researchTaskService.confirmResearchTask(id);
        return success(true);
    }

    @PutMapping("/archive-to-model")
    @Operation(summary = "归档到产品型号字典")
    @Parameter(name = "id", required = true)
    public CommonResult<Long> archiveToProductModel(@RequestParam("id") Long id) {
        return success(researchTaskService.archiveToProductModel(id));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除研发型号任务")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> deleteResearchTask(@RequestParam("id") Long id) {
        researchTaskService.deleteResearchTask(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除研发型号任务")
    @Parameter(name = "ids", required = true)
    public CommonResult<Boolean> deleteResearchTaskList(@RequestParam("ids") List<Long> ids) {
        researchTaskService.deleteResearchTaskListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得研发型号任务")
    @Parameter(name = "id", required = true)
    public CommonResult<HcResearchTaskRespVO> getResearchTask(@RequestParam("id") Long id) {
        HcResearchTaskDO entity = researchTaskService.getResearchTask(id);
        return success(BeanUtils.toBean(entity, HcResearchTaskRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得研发型号任务列表")
    public CommonResult<List<HcResearchTaskRespVO>> getResearchTaskList(@Valid HcResearchTaskPageReqVO reqVO) {
        List<HcResearchTaskDO> list = researchTaskService.getResearchTaskList(reqVO);
        return success(BeanUtils.toBean(list, HcResearchTaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得研发型号任务分页")
    public CommonResult<PageResult<HcResearchTaskRespVO>> getResearchTaskPage(@Valid HcResearchTaskPageReqVO pageReqVO) {
        PageResult<HcResearchTaskDO> pageResult = researchTaskService.getResearchTaskPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcResearchTaskRespVO.class));
    }

    @GetMapping("/preview-code")
    @Operation(summary = "预览研发型号编码")
    public CommonResult<HcResearchTaskCodePreviewRespVO> previewModelCode(
            @Valid HcResearchTaskCodePreviewReqVO reqVO) {
        return success(researchTaskService.previewModelCode(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出研发型号任务")
    @ApiAccessLog(operateType = EXPORT)
    public void exportResearchTaskExcel(@Valid HcResearchTaskPageReqVO pageReqVO, HttpServletResponse response)
            throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcResearchTaskDO> list = researchTaskService.getResearchTaskPage(pageReqVO).getList();
        ExcelUtils.write(response, "research-task.xls", "data", HcResearchTaskRespVO.class,
                BeanUtils.toBean(list, HcResearchTaskRespVO.class));
    }

}
