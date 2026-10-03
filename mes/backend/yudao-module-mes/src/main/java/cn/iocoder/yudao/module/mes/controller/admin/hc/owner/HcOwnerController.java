package cn.iocoder.yudao.module.mes.controller.admin.hc.owner;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.owner.HcOwnerDO;
import cn.iocoder.yudao.module.mes.service.hc.owner.HcOwnerService;
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

@Tag(name = "???? - 货主")
@RestController
@RequestMapping("/mes/hc/base/owner")
@Validated
public class HcOwnerController {

    @Resource
    private HcOwnerService hcOwnerService;

    @PostMapping("/create")
    @Operation(summary = "??货主")
    public CommonResult<Long> createHcOwner(@Valid @RequestBody HcOwnerSaveReqVO createReqVO) {
        return success(hcOwnerService.createHcOwner(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??货主")
    public CommonResult<Boolean> updateHcOwner(@Valid @RequestBody HcOwnerSaveReqVO updateReqVO) {
        hcOwnerService.updateHcOwner(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??货主")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcOwner(@RequestParam("id") Long id) {
        hcOwnerService.deleteHcOwner(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????货主")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcOwnerList(@RequestParam("ids") List<Long> ids) {
        hcOwnerService.deleteHcOwnerListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??货主??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcOwnerRespVO> getHcOwner(@RequestParam("id") Long id) {
        HcOwnerDO entity = hcOwnerService.getHcOwner(id);
        return success(BeanUtils.toBean(entity, HcOwnerRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??货主????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcOwnerDetailRespVO> getHcOwnerDetail(@RequestParam("id") Long id) {
        HcOwnerDO entity = hcOwnerService.getHcOwner(id);
        HcOwnerDetailRespVO respVO = BeanUtils.toBean(entity, HcOwnerDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??货主????")
    public CommonResult<List<HcOwnerSimpleRespVO>> getHcOwnerSimpleList() {
        List<HcOwnerDO> list = hcOwnerService.getHcOwnerSimpleList();
        return success(BeanUtils.toBean(list, HcOwnerSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??货主????")
    public CommonResult<Map<Long, String>> getHcOwnerSimpleMap() {
        List<HcOwnerDO> list = hcOwnerService.getHcOwnerSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getOwnerName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??货主????")
    public CommonResult<List<HcOwnerSelectOptionRespVO>> getHcOwnerSelectOptions() {
        List<HcOwnerDO> list = hcOwnerService.getHcOwnerSimpleList();
        List<HcOwnerSelectOptionRespVO> result = new ArrayList<>();
        for (HcOwnerDO item : list) {
            HcOwnerSelectOptionRespVO option = new HcOwnerSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getOwnerName());
            option.setCode(item.getOwnerCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??货主??")
    public CommonResult<List<HcOwnerRespVO>> getHcOwnerList(@Valid HcOwnerPageReqVO reqVO) {
        List<HcOwnerDO> list = hcOwnerService.getHcOwnerList(reqVO);
        return success(BeanUtils.toBean(list, HcOwnerRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??货主??")
    public CommonResult<PageResult<HcOwnerRespVO>> getHcOwnerPage(@Valid HcOwnerPageReqVO pageReqVO) {
        PageResult<HcOwnerDO> pageResult = hcOwnerService.getHcOwnerPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcOwnerRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??货主 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcOwnerExcel(@Valid HcOwnerPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcOwnerDO> list = hcOwnerService.getHcOwnerPage(pageReqVO).getList();
        ExcelUtils.write(response, "货主.xls", "??", HcOwnerRespVO.class, BeanUtils.toBean(list, HcOwnerRespVO.class));
    }

}