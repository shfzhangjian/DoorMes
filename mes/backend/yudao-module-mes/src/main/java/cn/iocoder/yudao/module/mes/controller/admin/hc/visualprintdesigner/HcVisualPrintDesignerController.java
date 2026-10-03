package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignAttachReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintFieldOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintImportRespVO;
import cn.iocoder.yudao.module.mes.service.hc.visualprintdesigner.HcVisualPrintDesignerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 可视化打印设计器")
@RestController
@RequestMapping("/mes/hc/execution/visual-print-designer")
@Validated
public class HcVisualPrintDesignerController {

    @Resource
    private HcVisualPrintDesignerService visualPrintDesignerService;

    @PostMapping("/create")
    @Operation(summary = "创建客户打印信息")
    public CommonResult<Long> createCustomerInfo(@Valid @RequestBody HcVisualPrintCustomerInfoSaveReqVO createReqVO) {
        return success(visualPrintDesignerService.createCustomerInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户打印信息")
    public CommonResult<Boolean> updateCustomerInfo(@Valid @RequestBody HcVisualPrintCustomerInfoSaveReqVO updateReqVO) {
        visualPrintDesignerService.updateCustomerInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户打印信息")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteCustomerInfo(@RequestParam("id") Long id,
            @RequestParam(value = "productItemId", required = false) Long productItemId) {
        visualPrintDesignerService.deleteCustomerInfo(id, productItemId);
        return success(true);
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得客户打印信息详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcVisualPrintCustomerInfoRespVO> getCustomerInfoDetail(@RequestParam("id") Long id) {
        return success(visualPrintDesignerService.getCustomerInfoDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户打印信息分页")
    public CommonResult<PageResult<HcVisualPrintCustomerInfoRespVO>> getCustomerInfoPage(
            @Valid HcVisualPrintCustomerInfoPageReqVO pageReqVO) {
        return success(visualPrintDesignerService.getCustomerInfoPage(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获得客户打印信息列表")
    public CommonResult<List<HcVisualPrintCustomerInfoRespVO>> getCustomerInfoList(
            @Valid HcVisualPrintCustomerInfoPageReqVO reqVO) {
        return success(visualPrintDesignerService.getCustomerInfoList(reqVO));
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入客户打印信息Excel")
    public CommonResult<HcVisualPrintImportRespVO> importCustomerInfoExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        return success(visualPrintDesignerService.importCustomerInfoExcel(file));
    }

    @GetMapping("/field-options")
    @Operation(summary = "获得设计器字段选项")
    public CommonResult<List<HcVisualPrintFieldOptionRespVO>> getFieldOptions() {
        return success(visualPrintDesignerService.getFieldOptions());
    }

    @GetMapping("/designs")
    @Operation(summary = "获得客户打印信息设计稿")
    @Parameter(name = "customerInfoId", description = "客户打印信息ID", required = true)
    public CommonResult<List<HcVisualPrintDesignRespVO>> getDesigns(
            @RequestParam("customerInfoId") Long customerInfoId,
            @RequestParam(value = "productItemId", required = false) Long productItemId) {
        return success(visualPrintDesignerService.getDesigns(customerInfoId, productItemId));
    }

    @GetMapping("/design")
    @Operation(summary = "获得单个标签设计稿")
    public CommonResult<HcVisualPrintDesignRespVO> getDesign(@RequestParam("customerInfoId") Long customerInfoId,
            @RequestParam(value = "productItemId", required = false) Long productItemId,
            @RequestParam("labelKind") String labelKind) {
        return success(visualPrintDesignerService.getDesign(customerInfoId, productItemId, labelKind));
    }

    @PostMapping("/design/save")
    @Operation(summary = "保存可视化打印设计稿")
    public CommonResult<Long> saveDesign(@Valid @RequestBody HcVisualPrintDesignSaveReqVO saveReqVO) {
        return success(visualPrintDesignerService.saveDesign(saveReqVO));
    }

    @PostMapping("/design/attach")
    @Operation(summary = "挂接设计稿到产品型号尺寸")
    public CommonResult<HcVisualPrintDesignRespVO> attachDesign(
            @Valid @RequestBody HcVisualPrintDesignAttachReqVO attachReqVO) {
        return success(visualPrintDesignerService.attachDesign(attachReqVO));
    }

}
