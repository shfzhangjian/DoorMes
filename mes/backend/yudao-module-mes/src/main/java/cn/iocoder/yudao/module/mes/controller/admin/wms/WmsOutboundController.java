// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.wms.WmsOutboundController.java
package cn.iocoder.yudao.module.mes.controller.admin.wms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsOutboundVOs.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsOutboundDO;
import cn.iocoder.yudao.module.mes.service.wms.WmsOutboundServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - WMS出库与领料单")
@RestController
@RequestMapping("/mes/wms-outbound")
public class WmsOutboundController {
    @Resource
    private WmsOutboundServiceImpl wmsOutboundService;

    @PostMapping("/create")
    @Operation(summary = "创建出库/领料单")
    public CommonResult<Long> createOutbound(@Valid @RequestBody SaveReqVO createReqVO) {
        return success(wmsOutboundService.createOutbound(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "出库单分页")
    public CommonResult<PageResult<WmsOutboundDO>> getOutboundPage(@Valid PageReqVO pageVO) {
        return success(wmsOutboundService.getOutboundPage(pageVO));
    }
}
