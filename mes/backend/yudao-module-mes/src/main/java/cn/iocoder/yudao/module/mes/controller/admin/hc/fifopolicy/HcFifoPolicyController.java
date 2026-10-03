package cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicyDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicySelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicySimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.fifopolicy.HcFifoPolicyDO;
import cn.iocoder.yudao.module.mes.service.hc.fifopolicy.HcFifoPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "???? - 先进先出策略")
@RestController
@RequestMapping("/mes/hc/base/fifo-policy")
@Validated
public class HcFifoPolicyController {

    @Resource
    private HcFifoPolicyService hcFifoPolicyService;

    @PostMapping("/create")
    @Operation(summary = "??先进先出策略")
    public CommonResult<Long> createHcFifoPolicy(@Valid @RequestBody HcFifoPolicySaveReqVO createReqVO) {
        return success(hcFifoPolicyService.createHcFifoPolicy(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??先进先出策略")
    public CommonResult<Boolean> updateHcFifoPolicy(@Valid @RequestBody HcFifoPolicySaveReqVO updateReqVO) {
        hcFifoPolicyService.updateHcFifoPolicy(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??先进先出策略")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcFifoPolicy(@RequestParam("id") Long id) {
        hcFifoPolicyService.deleteHcFifoPolicy(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????先进先出策略")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcFifoPolicyList(@RequestParam("ids") List<Long> ids) {
        hcFifoPolicyService.deleteHcFifoPolicyListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??先进先出策略??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcFifoPolicyRespVO> getHcFifoPolicy(@RequestParam("id") Long id) {
        HcFifoPolicyDO entity = hcFifoPolicyService.getHcFifoPolicy(id);
        return success(BeanUtils.toBean(entity, HcFifoPolicyRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??先进先出策略????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcFifoPolicyDetailRespVO> getHcFifoPolicyDetail(@RequestParam("id") Long id) {
        HcFifoPolicyDO entity = hcFifoPolicyService.getHcFifoPolicy(id);
        HcFifoPolicyDetailRespVO respVO = BeanUtils.toBean(entity, HcFifoPolicyDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??先进先出策略????")
    public CommonResult<List<HcFifoPolicySimpleRespVO>> getHcFifoPolicySimpleList() {
        List<HcFifoPolicyDO> list = hcFifoPolicyService.getHcFifoPolicySimpleList();
        return success(BeanUtils.toBean(list, HcFifoPolicySimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??先进先出策略????")
    public CommonResult<Map<Long, String>> getHcFifoPolicySimpleMap() {
        List<HcFifoPolicyDO> list = hcFifoPolicyService.getHcFifoPolicySimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getPolicyName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??先进先出策略????")
    public CommonResult<List<HcFifoPolicySelectOptionRespVO>> getHcFifoPolicySelectOptions() {
        List<HcFifoPolicyDO> list = hcFifoPolicyService.getHcFifoPolicySimpleList();
        List<HcFifoPolicySelectOptionRespVO> result = new ArrayList<>();
        for (HcFifoPolicyDO item : list) {
            HcFifoPolicySelectOptionRespVO option = new HcFifoPolicySelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getPolicyName());
            option.setCode(item.getPolicyCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??先进先出策略??")
    public CommonResult<List<HcFifoPolicyRespVO>> getHcFifoPolicyList(@Valid HcFifoPolicyPageReqVO reqVO) {
        List<HcFifoPolicyDO> list = hcFifoPolicyService.getHcFifoPolicyList(reqVO);
        return success(BeanUtils.toBean(list, HcFifoPolicyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??先进先出策略??")
    public CommonResult<PageResult<HcFifoPolicyRespVO>> getHcFifoPolicyPage(@Valid HcFifoPolicyPageReqVO pageReqVO) {
        PageResult<HcFifoPolicyDO> pageResult = hcFifoPolicyService.getHcFifoPolicyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcFifoPolicyRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??先进先出策略 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcFifoPolicyExcel(@Valid HcFifoPolicyPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcFifoPolicyDO> list = hcFifoPolicyService.getHcFifoPolicyPage(pageReqVO).getList();
        ExcelUtils.write(response, "先进先出策略.xls", "??", HcFifoPolicyRespVO.class, BeanUtils.toBean(list, HcFifoPolicyRespVO.class));
    }

}