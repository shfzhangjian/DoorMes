package cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeStatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeStateRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processmateriallife.HcProcessMaterialLifeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工序耗材在用状态")
@RestController
@RequestMapping("/mes/hc/base/process-material-life")
@Validated
public class HcProcessMaterialLifeController {

    @Resource
    private HcProcessMaterialLifeService hcProcessMaterialLifeService;

    @GetMapping("/state-page")
    @Operation(summary = "工序耗材在用状态分页")
    public CommonResult<PageResult<HcProcessMaterialLifeStateRespVO>> statePage(
            @Valid HcProcessMaterialLifeStatePageReqVO reqVO) {
        return success(hcProcessMaterialLifeService.getStatePage(reqVO));
    }

    @GetMapping("/event-page")
    @Operation(summary = "工序耗材使用更换流水分页")
    public CommonResult<PageResult<HcProcessMaterialLifeEventRespVO>> eventPage(
            @Valid HcProcessMaterialLifeEventPageReqVO reqVO) {
        return success(hcProcessMaterialLifeService.getEventPage(reqVO));
    }

    @PostMapping("/event/create")
    @Operation(summary = "新增工序耗材在用状态手工流水")
    public CommonResult<Long> createEvent(@Valid @RequestBody HcProcessMaterialLifeEventSaveReqVO reqVO) {
        return success(hcProcessMaterialLifeService.createEvent(reqVO));
    }
}
