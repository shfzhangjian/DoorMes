package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsExceptionReviewConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "管理后台 - 异常事件会签配置")
@RestController
@RequestMapping("/mes/qms-exception-review-config")
@Validated
public class QmsExceptionReviewConfigController {

    @Resource
    private QmsExceptionReviewConfigService qmsExceptionReviewConfigService;

    @GetMapping("/page")
    @Operation(summary = "获得异常事件临时小组配置分页")
    public CommonResult<PageResult<QmsNcReviewConfigRespVO>> getReviewConfigPage(
            @Valid QmsNcReviewConfigPageReqVO pageReqVO) {
        return success(qmsExceptionReviewConfigService.getReviewConfigPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的异常事件临时小组配置")
    public CommonResult<List<QmsNcReviewConfigRespVO>> getSimpleList() {
        return success(qmsExceptionReviewConfigService.getSimpleList());
    }

    @GetMapping("/get")
    @Operation(summary = "获得异常事件临时小组配置详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsNcReviewConfigRespVO> getReviewConfig(@RequestParam("id") Long id) {
        return success(qmsExceptionReviewConfigService.getReviewConfig(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建异常事件临时小组配置")
    public CommonResult<Long> createReviewConfig(@Valid @RequestBody QmsNcReviewConfigSaveReqVO createReqVO) {
        return success(qmsExceptionReviewConfigService.createReviewConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新异常事件临时小组配置")
    public CommonResult<Boolean> updateReviewConfig(@Valid @RequestBody QmsNcReviewConfigSaveReqVO updateReqVO) {
        qmsExceptionReviewConfigService.updateReviewConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除异常事件临时小组配置")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteReviewConfig(@RequestParam("id") Long id) {
        qmsExceptionReviewConfigService.deleteReviewConfig(id);
        return success(true);
    }
}
