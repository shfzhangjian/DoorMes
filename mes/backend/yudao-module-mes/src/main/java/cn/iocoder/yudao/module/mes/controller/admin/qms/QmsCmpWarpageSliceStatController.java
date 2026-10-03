package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceImportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsCmpWarpageSliceStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - CMP软垫翘曲片号统计")
@RestController
@RequestMapping("/mes/quality/statistics/cmp-warpage-slice-stat")
@Validated
public class QmsCmpWarpageSliceStatController {

    @Resource
    private QmsCmpWarpageSliceStatService qmsCmpWarpageSliceStatService;

    @GetMapping("/page")
    @Operation(summary = "获得CMP软垫翘曲片号统计分页")
    public CommonResult<PageResult<QmsCmpWarpageSliceStatRespVO>> getPage(
            @Valid QmsCmpWarpageSliceStatPageReqVO reqVO) {
        return success(qmsCmpWarpageSliceStatService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得CMP软垫翘曲片号统计详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsCmpWarpageSliceStatRespVO> get(@RequestParam("id") Long id) {
        return success(qmsCmpWarpageSliceStatService.get(id));
    }

    @PostMapping("/create")
    @Operation(summary = "新增CMP软垫翘曲片号统计")
    public CommonResult<Long> create(@Valid @RequestBody QmsCmpWarpageSliceStatSaveReqVO reqVO) {
        return success(qmsCmpWarpageSliceStatService.create(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新CMP软垫翘曲片号统计")
    public CommonResult<Boolean> update(@Valid @RequestBody QmsCmpWarpageSliceStatUpdateReqVO reqVO) {
        qmsCmpWarpageSliceStatService.update(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除CMP软垫翘曲片号统计")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        qmsCmpWarpageSliceStatService.delete(id);
        return success(true);
    }

    @PostMapping("/sync")
    @Operation(summary = "按生产片号同步裁切FQC翘曲值和客户片号")
    public CommonResult<QmsCmpWarpageSliceStatSyncRespVO> sync(
            @Valid @RequestBody QmsCmpWarpageSliceStatSyncReqVO reqVO) {
        return success(qmsCmpWarpageSliceStatService.sync(reqVO));
    }

    @PostMapping("/sync-slices")
    @Operation(summary = "从裁切送检明细增量同步生产片号")
    public CommonResult<QmsCmpWarpageSliceImportRespVO> syncSlices(
            @Valid @RequestBody QmsCmpWarpageSliceImportReqVO reqVO) {
        return success(qmsCmpWarpageSliceStatService.syncSlices(reqVO));
    }
}
