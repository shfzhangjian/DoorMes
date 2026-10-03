// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.wms.WmsInboundController.java
package cn.iocoder.yudao.module.mes.controller.admin.wms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsInboundDO;
import cn.iocoder.yudao.module.mes.service.wms.WmsInboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - WMS入库单")
@RestController
@RequestMapping("/mes/wms-inbound")
@Validated
public class WmsInboundController {

    @Resource
    private WmsInboundService wmsInboundService;

    @PostMapping("/create")
    @Operation(summary = "创建入库单")
    public CommonResult<Long> createInbound(@Valid @RequestBody WmsInboundSaveReqVO createReqVO) {
        return success(wmsInboundService.createInbound(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改入库单")
    public CommonResult<Boolean> updateInbound(@Valid @RequestBody WmsInboundSaveReqVO updateReqVO) {
        wmsInboundService.updateInbound(updateReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得入库单分页")
    public CommonResult<PageResult<WmsInboundDO>> getInboundPage(@Valid WmsInboundPageReqVO pageVO) {
        return success(wmsInboundService.getInboundPage(pageVO));
    }
}
