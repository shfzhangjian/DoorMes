package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategoryListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCategoryDO;
import cn.iocoder.yudao.module.mes.service.qms.measuretool.QmsMeasureToolService;
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

@Tag(name = "管理后台 - 量检具分类")
@RestController
@RequestMapping("/mes/quality/measure-tool/category")
@Validated
public class QmsMeasureToolCategoryController {

    @Resource
    private QmsMeasureToolService measureToolService;

    @PostMapping("/create")
    @Operation(summary = "创建量检具分类")
    public CommonResult<Long> createCategory(@Valid @RequestBody QmsMeasureToolCategorySaveReqVO reqVO) {
        return success(measureToolService.createCategory(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新量检具分类")
    public CommonResult<Boolean> updateCategory(
            @Validated({Default.class, QmsMeasureToolCategorySaveReqVO.Update.class})
            @RequestBody QmsMeasureToolCategorySaveReqVO reqVO) {
        measureToolService.updateCategory(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除量检具分类")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteCategory(@RequestParam("id") Long id) {
        measureToolService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得量检具分类")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsMeasureToolCategoryRespVO> getCategory(@RequestParam("id") Long id) {
        QmsMeasureToolCategoryDO category = measureToolService.getCategory(id);
        return success(BeanUtils.toBean(category, QmsMeasureToolCategoryRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得量检具分类列表")
    public CommonResult<List<QmsMeasureToolCategoryRespVO>> getCategoryList(@Valid QmsMeasureToolCategoryListReqVO reqVO) {
        return success(BeanUtils.toBean(measureToolService.getCategoryList(reqVO), QmsMeasureToolCategoryRespVO.class));
    }

}
