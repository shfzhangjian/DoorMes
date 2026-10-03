package cn.iocoder.yudao.module.mes.controller.admin.hc.terminal;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.terminal.HcTerminalDO;
import cn.iocoder.yudao.module.mes.service.hc.terminal.HcTerminalService;
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

@Tag(name = "???? - 终端工位")
@RestController
@RequestMapping("/mes/hc/base/terminal")
@Validated
public class HcTerminalController {

    @Resource
    private HcTerminalService hcTerminalService;

    @PostMapping("/create")
    @Operation(summary = "??终端工位")
    public CommonResult<Long> createHcTerminal(@Valid @RequestBody HcTerminalSaveReqVO createReqVO) {
        return success(hcTerminalService.createHcTerminal(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??终端工位")
    public CommonResult<Boolean> updateHcTerminal(@Valid @RequestBody HcTerminalSaveReqVO updateReqVO) {
        hcTerminalService.updateHcTerminal(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??终端工位")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcTerminal(@RequestParam("id") Long id) {
        hcTerminalService.deleteHcTerminal(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????终端工位")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcTerminalList(@RequestParam("ids") List<Long> ids) {
        hcTerminalService.deleteHcTerminalListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??终端工位??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcTerminalRespVO> getHcTerminal(@RequestParam("id") Long id) {
        HcTerminalDO entity = hcTerminalService.getHcTerminal(id);
        return success(BeanUtils.toBean(entity, HcTerminalRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??终端工位????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcTerminalDetailRespVO> getHcTerminalDetail(@RequestParam("id") Long id) {
        HcTerminalDO entity = hcTerminalService.getHcTerminal(id);
        HcTerminalDetailRespVO respVO = BeanUtils.toBean(entity, HcTerminalDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??终端工位????")
    public CommonResult<List<HcTerminalSimpleRespVO>> getHcTerminalSimpleList() {
        List<HcTerminalDO> list = hcTerminalService.getHcTerminalSimpleList();
        return success(BeanUtils.toBean(list, HcTerminalSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??终端工位????")
    public CommonResult<Map<Long, String>> getHcTerminalSimpleMap() {
        List<HcTerminalDO> list = hcTerminalService.getHcTerminalSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getTerminalName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??终端工位????")
    public CommonResult<List<HcTerminalSelectOptionRespVO>> getHcTerminalSelectOptions() {
        List<HcTerminalDO> list = hcTerminalService.getHcTerminalSimpleList();
        List<HcTerminalSelectOptionRespVO> result = new ArrayList<>();
        for (HcTerminalDO item : list) {
            HcTerminalSelectOptionRespVO option = new HcTerminalSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getTerminalName());
            option.setCode(item.getTerminalCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??终端工位??")
    public CommonResult<List<HcTerminalRespVO>> getHcTerminalList(@Valid HcTerminalPageReqVO reqVO) {
        List<HcTerminalDO> list = hcTerminalService.getHcTerminalList(reqVO);
        return success(BeanUtils.toBean(list, HcTerminalRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??终端工位??")
    public CommonResult<PageResult<HcTerminalRespVO>> getHcTerminalPage(@Valid HcTerminalPageReqVO pageReqVO) {
        PageResult<HcTerminalDO> pageResult = hcTerminalService.getHcTerminalPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcTerminalRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??终端工位 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcTerminalExcel(@Valid HcTerminalPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcTerminalDO> list = hcTerminalService.getHcTerminalPage(pageReqVO).getList();
        ExcelUtils.write(response, "终端工位.xls", "??", HcTerminalRespVO.class, BeanUtils.toBean(list, HcTerminalRespVO.class));
    }

}