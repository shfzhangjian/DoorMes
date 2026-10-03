// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.wms.WmsStockController.java
package cn.iocoder.yudao.module.mes.controller.admin.wms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsStockDO;
import cn.iocoder.yudao.module.mes.service.wms.WmsStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - WMS实时库存查询")
@RestController
@RequestMapping("/mes/wms-stock")
@Validated
public class WmsStockController {

    @Resource
    private WmsStockService wmsStockService;

    @GetMapping("/page")
    @Operation(summary = "获得实时库存分页")
    public CommonResult<PageResult<WmsStockDO>> getStockPage(@Valid WmsStockPageReqVO pageVO) {
        return success(wmsStockService.getStockPage(pageVO));
    }
}
