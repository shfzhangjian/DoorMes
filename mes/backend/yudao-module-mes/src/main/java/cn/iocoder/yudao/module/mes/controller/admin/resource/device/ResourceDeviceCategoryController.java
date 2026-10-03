package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategoryListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceCategoryDO;
import cn.iocoder.yudao.module.mes.service.resource.device.ResourceDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

@Tag(name = "管理后台 - 设备分类")
@RestController
@RequestMapping("/mes/resource/device/category")
@Validated
public class ResourceDeviceCategoryController {

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @PostMapping("/create")
    @Operation(summary = "创建设备分类")
    public CommonResult<Long> createCategory(@Valid @RequestBody ResourceDeviceCategorySaveReqVO reqVO) {
        return success(resourceDeviceService.createCategory(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备分类")
    public CommonResult<Boolean> updateCategory(
            @Validated({Default.class, ResourceDeviceCategorySaveReqVO.Update.class})
            @RequestBody ResourceDeviceCategorySaveReqVO reqVO) {
        resourceDeviceService.updateCategory(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备分类")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteCategory(@RequestParam("id") Long id) {
        resourceDeviceService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备分类")
    public CommonResult<ResourceDeviceCategoryRespVO> getCategory(@RequestParam("id") Long id) {
        ResourceDeviceCategoryDO category = resourceDeviceService.getCategory(id);
        return success(BeanUtils.toBean(category, ResourceDeviceCategoryRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得设备分类列表")
    public CommonResult<List<ResourceDeviceCategoryRespVO>> getCategoryList(@Valid ResourceDeviceCategoryListReqVO reqVO) {
        return success(BeanUtils.toBean(resourceDeviceService.getCategoryList(reqVO), ResourceDeviceCategoryRespVO.class));
    }

}
