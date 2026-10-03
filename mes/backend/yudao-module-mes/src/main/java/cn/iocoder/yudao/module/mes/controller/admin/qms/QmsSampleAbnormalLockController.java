package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsSampleAbnormalLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 留样异常复检")
@RestController
@RequestMapping("/mes/quality/sample-abnormal-recheck")
public class QmsSampleAbnormalLockController {

    @Resource
    private QmsSampleAbnormalLockService sampleAbnormalLockService;

    @GetMapping("/page")
    @Operation(summary = "获得留样异常复检分页")
    public CommonResult<PageResult<QmsSampleAbnormalLockRespVO>> getPage(
            @Valid QmsSampleAbnormalLockPageReqVO pageReqVO) {
        return success(sampleAbnormalLockService.getPage(pageReqVO));
    }

    @GetMapping("/active")
    @Operation(summary = "获得当前工序对象生效中的留样异常锁")
    public CommonResult<QmsSampleAbnormalLockRespVO> getActiveLock(
            @RequestParam("sourceProcessCode") String sourceProcessCode,
            @RequestParam("objectType") String objectType,
            @RequestParam("objectNo") String objectNo,
            @RequestParam(value = "qualificationObjectNo", required = false) String qualificationObjectNo) {
        return success(sampleAbnormalLockService.getActiveLock(sourceProcessCode, objectType, objectNo, qualificationObjectNo));
    }

    @PostMapping("/{id}/recheck")
    @Operation(summary = "创建留样异常复检单")
    public CommonResult<QmsSampleAbnormalLockRespVO> createRecheck(@PathVariable("id") Long id) {
        return success(sampleAbnormalLockService.createRecheck(id));
    }
}
