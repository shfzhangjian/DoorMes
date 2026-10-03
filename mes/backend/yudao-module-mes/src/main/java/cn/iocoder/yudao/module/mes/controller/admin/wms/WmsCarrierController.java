// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.wms.WmsCarrierController.java
package cn.iocoder.yudao.module.mes.controller.admin.wms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsCarrierPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsCarrierSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsCarrierDO;
import cn.iocoder.yudao.module.mes.service.wms.WmsCarrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 载具与周转箱台账")
@RestController
@RequestMapping("/mes/wms-carrier")
@Validated
public class WmsCarrierController {

    @Resource
    private WmsCarrierService wmsCarrierService;

    @PostMapping("/create")
    @Operation(summary = "创建载具记录")
    public CommonResult<Long> createCarrier(@Valid @RequestBody WmsCarrierSaveReqVO createReqVO) {
        return success(wmsCarrierService.createCarrier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改载具记录(或更新状态位置)")
    public CommonResult<Boolean> updateCarrier(@Valid @RequestBody WmsCarrierSaveReqVO updateReqVO) {
        wmsCarrierService.updateCarrier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除载具")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteCarrier(@RequestParam("id") Long id) {
        wmsCarrierService.deleteCarrier(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得载具详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<WmsCarrierDO> getCarrier(@RequestParam("id") Long id) {
        return success(wmsCarrierService.getCarrier(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得载具分页")
    public CommonResult<PageResult<WmsCarrierDO>> getCarrierPage(@Valid WmsCarrierPageReqVO pageVO) {
        return success(wmsCarrierService.getCarrierPage(pageVO));
    }
}
