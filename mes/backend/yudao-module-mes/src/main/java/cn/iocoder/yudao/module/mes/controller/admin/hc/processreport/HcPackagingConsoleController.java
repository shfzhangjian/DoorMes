package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingAddInnerItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingAddOuterUnitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingCreateInnerUnitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingCreateOuterBoxReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingInnerUnitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingOuterBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingUnitActionReqVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcPackagingConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 包装操作看板")
@RestController
@RequestMapping("/mes/hc/execution/formula-report/packaging")
@Validated
public class HcPackagingConsoleController {

    @Resource
    private HcPackagingConsoleService hcPackagingConsoleService;

    @GetMapping("/task-list")
    @Operation(summary = "获得包装操作看板任务列表")
    public CommonResult<List<HcPackagingTaskRespVO>> getTaskList(@Valid HcPackagingTaskPageReqVO reqVO) {
        return success(hcPackagingConsoleService.getTaskList(reqVO));
    }

    @PostMapping("/start")
    @Operation(summary = "执行包装开工确认")
    public CommonResult<Long> start(@Valid @RequestBody HcPackagingStartReqVO reqVO) {
        return success(hcPackagingConsoleService.start(reqVO));
    }

    @GetMapping("/pass-work/list")
    @Operation(summary = "获得包装点检/清洁列表")
    public CommonResult<List<HcPackagingPassWorkRespVO>> getPassWorkList(@RequestParam("planId") Long planId,
                                                                         @RequestParam("planOperationId") Long planOperationId) {
        return success(hcPackagingConsoleService.getPassWorkList(planId, planOperationId));
    }

    @PostMapping("/pass-work/save")
    @Operation(summary = "保存包装点检/清洁")
    public CommonResult<Long> savePassWork(@Valid @RequestBody HcPackagingPassWorkSaveReqVO reqVO) {
        return success(hcPackagingConsoleService.savePassWork(reqVO));
    }

    @PostMapping("/pass-work/confirm")
    @Operation(summary = "确认包装点检/清洁")
    public CommonResult<Long> confirmPassWork(@Valid @RequestBody HcPackagingPassWorkSaveReqVO reqVO) {
        return success(hcPackagingConsoleService.confirmPassWork(reqVO));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得包装看板汇总")
    public CommonResult<HcPackagingSummaryRespVO> getSummary(@RequestParam("planId") Long planId,
                                                            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcPackagingConsoleService.getSummary(planId, planOperationId));
    }

    @GetMapping("/source/list")
    @Operation(summary = "获得包装来源裁切片号")
    public CommonResult<List<HcPackagingSourceRespVO>> getSourceList(@RequestParam("planId") Long planId,
                                                                     @RequestParam("planOperationId") Long planOperationId) {
        return success(hcPackagingConsoleService.getSourceList(planId, planOperationId));
    }

    @GetMapping("/source/scan")
    @Operation(summary = "扫码查询包装来源裁切片号")
    public CommonResult<HcPackagingSourceRespVO> scanSource(@RequestParam("sliceBatchNo") String sliceBatchNo) {
        return success(hcPackagingConsoleService.scanSource(sliceBatchNo));
    }

    @GetMapping("/inner-unit/list")
    @Operation(summary = "获得内包装单元列表")
    public CommonResult<List<HcPackagingInnerUnitRespVO>> getInnerUnitList(@RequestParam("planOperationId") Long planOperationId) {
        return success(hcPackagingConsoleService.getInnerUnitList(planOperationId));
    }

    @PostMapping("/inner-unit/create")
    @Operation(summary = "创建内包装单元")
    public CommonResult<Long> createInnerUnit(@Valid @RequestBody HcPackagingCreateInnerUnitReqVO reqVO) {
        return success(hcPackagingConsoleService.createInnerUnit(reqVO));
    }

    @PostMapping("/inner-unit/add-item")
    @Operation(summary = "内包装扫描加入裁切片号")
    public CommonResult<Long> addInnerItem(@Valid @RequestBody HcPackagingAddInnerItemReqVO reqVO) {
        return success(hcPackagingConsoleService.addInnerItem(reqVO));
    }

    @PostMapping("/inner-unit/confirm")
    @Operation(summary = "确认内包装单元")
    public CommonResult<Long> confirmInnerUnit(@Valid @RequestBody HcPackagingUnitActionReqVO reqVO) {
        return success(hcPackagingConsoleService.confirmInnerUnit(reqVO));
    }

    @PostMapping("/inner-unit/print")
    @Operation(summary = "打印内包装标签")
    public CommonResult<Long> printInnerUnit(@Valid @RequestBody HcPackagingUnitActionReqVO reqVO) {
        return success(hcPackagingConsoleService.printInnerUnit(reqVO));
    }

    @PostMapping("/inner-unit/review")
    @Operation(summary = "复核内包装标签")
    public CommonResult<Long> reviewInnerUnit(@Valid @RequestBody HcPackagingUnitActionReqVO reqVO) {
        return success(hcPackagingConsoleService.reviewInnerUnit(reqVO));
    }

    @GetMapping("/outer-box/list")
    @Operation(summary = "获得外包装箱/板列表")
    public CommonResult<List<HcPackagingOuterBoxRespVO>> getOuterBoxList(@RequestParam("planOperationId") Long planOperationId) {
        return success(hcPackagingConsoleService.getOuterBoxList(planOperationId));
    }

    @PostMapping("/outer-box/create")
    @Operation(summary = "创建外包装箱/板")
    public CommonResult<Long> createOuterBox(@Valid @RequestBody HcPackagingCreateOuterBoxReqVO reqVO) {
        return success(hcPackagingConsoleService.createOuterBox(reqVO));
    }

    @PostMapping("/outer-box/add-unit")
    @Operation(summary = "外包装扫描加入内包装单元")
    public CommonResult<Long> addOuterUnit(@Valid @RequestBody HcPackagingAddOuterUnitReqVO reqVO) {
        return success(hcPackagingConsoleService.addOuterUnit(reqVO));
    }

    @PostMapping("/outer-box/confirm")
    @Operation(summary = "确认外包装箱/板")
    public CommonResult<Long> confirmOuterBox(@Valid @RequestBody HcPackagingUnitActionReqVO reqVO) {
        return success(hcPackagingConsoleService.confirmOuterBox(reqVO));
    }

    @PostMapping("/outer-box/print")
    @Operation(summary = "打印外包装标签")
    public CommonResult<Long> printOuterBox(@Valid @RequestBody HcPackagingUnitActionReqVO reqVO) {
        return success(hcPackagingConsoleService.printOuterBox(reqVO));
    }

    @PostMapping("/outer-box/review")
    @Operation(summary = "复核外包装标签")
    public CommonResult<Long> reviewOuterBox(@Valid @RequestBody HcPackagingUnitActionReqVO reqVO) {
        return success(hcPackagingConsoleService.reviewOuterBox(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交包装工单完工")
    public CommonResult<Long> submit(@Valid @RequestBody HcPackagingSubmitReqVO reqVO) {
        return success(hcPackagingConsoleService.submit(reqVO));
    }
}
