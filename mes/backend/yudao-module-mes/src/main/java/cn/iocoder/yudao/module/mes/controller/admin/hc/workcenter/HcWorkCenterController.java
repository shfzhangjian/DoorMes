package cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterSimpleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterTerminalRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter.HcWorkCenterDO;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcWorkCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
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

@Tag(name = "???? - 工作中心")
@RestController
@RequestMapping("/mes/hc/base/work-center")
@Validated
public class HcWorkCenterController {

    @Resource
    private HcWorkCenterService hcWorkCenterService;

    public static final String WORKSTATION_IP_HEADER = "X-HC-Workstation-IP";

    @PostMapping("/create")
    @Operation(summary = "??工作中心")
    public CommonResult<Long> createHcWorkCenter(@Valid @RequestBody HcWorkCenterSaveReqVO createReqVO) {
        return success(hcWorkCenterService.createHcWorkCenter(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??工作中心")
    public CommonResult<Boolean> updateHcWorkCenter(@Valid @RequestBody HcWorkCenterSaveReqVO updateReqVO) {
        hcWorkCenterService.updateHcWorkCenter(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??工作中心")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcWorkCenter(@RequestParam("id") Long id) {
        hcWorkCenterService.deleteHcWorkCenter(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????工作中心")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcWorkCenterList(@RequestParam("ids") List<Long> ids) {
        hcWorkCenterService.deleteHcWorkCenterListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??工作中心??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcWorkCenterRespVO> getHcWorkCenter(@RequestParam("id") Long id) {
        HcWorkCenterDO entity = hcWorkCenterService.getHcWorkCenter(id);
        return success(BeanUtils.toBean(entity, HcWorkCenterRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??工作中心????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcWorkCenterDetailRespVO> getHcWorkCenterDetail(@RequestParam("id") Long id) {
        HcWorkCenterDO entity = hcWorkCenterService.getHcWorkCenter(id);
        HcWorkCenterDetailRespVO respVO = BeanUtils.toBean(entity, HcWorkCenterDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping("/public/by-ip")
    @Operation(summary = "按工位 IP 获取工作中心和产线信息")
    @PermitAll
    public CommonResult<HcWorkCenterTerminalRespVO> getHcWorkCenterByIp(
            @RequestHeader(value = WORKSTATION_IP_HEADER, required = false) String headerIp,
            @RequestParam(value = "ip", required = false) String queryIp) {
        String ip = firstNotBlank(headerIp, queryIp);
        HcWorkCenterDO entity = hcWorkCenterService.getHcWorkCenterByTerminalIp(ip);
        HcWorkCenterTerminalRespVO respVO = new HcWorkCenterTerminalRespVO();
        respVO.setIp(ip);
        respVO.setMatched(entity != null);
        if (entity != null) {
            respVO.setWorkCenterId(entity.getId());
            respVO.setWorkCenterCode(entity.getWcCode());
            respVO.setWorkCenterName(entity.getWcName());
            respVO.setProcessCode(entity.getProcessCode());
            respVO.setProcessName(entity.getProcessName());
            respVO.setLineCode(entity.getLineCode());
            respVO.setLineName(entity.getLineName());
            respVO.setLineShortCode(entity.getLineShortCode());
            respVO.setBatchLineCode(entity.getBatchLineCode());
            respVO.setTerminalIps(entity.getTerminalIps());
        }
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??工作中心????")
    public CommonResult<List<HcWorkCenterSimpleRespVO>> getHcWorkCenterSimpleList() {
        List<HcWorkCenterDO> list = hcWorkCenterService.getHcWorkCenterSimpleList();
        return success(BeanUtils.toBean(list, HcWorkCenterSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??工作中心????")
    public CommonResult<Map<Long, String>> getHcWorkCenterSimpleMap() {
        List<HcWorkCenterDO> list = hcWorkCenterService.getHcWorkCenterSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getWcName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??工作中心????")
    public CommonResult<List<HcWorkCenterSelectOptionRespVO>> getHcWorkCenterSelectOptions() {
        List<HcWorkCenterDO> list = hcWorkCenterService.getHcWorkCenterSimpleList();
        List<HcWorkCenterSelectOptionRespVO> result = new ArrayList<>();
        for (HcWorkCenterDO item : list) {
            HcWorkCenterSelectOptionRespVO option = new HcWorkCenterSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getWcName());
            option.setCode(item.getWcCode());
            option.setProcessStage(item.getProcessStage());
            option.setProcessId(item.getProcessId());
            option.setProcessCode(item.getProcessCode());
            option.setProcessName(item.getProcessName());
            option.setLineCode(item.getLineCode());
            option.setLineName(item.getLineName());
            option.setLineShortCode(item.getLineShortCode());
            option.setBatchLineCode(item.getBatchLineCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??工作中心??")
    public CommonResult<List<HcWorkCenterRespVO>> getHcWorkCenterList(@Valid HcWorkCenterPageReqVO reqVO) {
        List<HcWorkCenterDO> list = hcWorkCenterService.getHcWorkCenterList(reqVO);
        return success(BeanUtils.toBean(list, HcWorkCenterRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??工作中心??")
    public CommonResult<PageResult<HcWorkCenterRespVO>> getHcWorkCenterPage(@Valid HcWorkCenterPageReqVO pageReqVO) {
        PageResult<HcWorkCenterDO> pageResult = hcWorkCenterService.getHcWorkCenterPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcWorkCenterRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??工作中心 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcWorkCenterExcel(@Valid HcWorkCenterPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcWorkCenterDO> list = hcWorkCenterService.getHcWorkCenterPage(pageReqVO).getList();
        ExcelUtils.write(response, "工作中心.xls", "??", HcWorkCenterRespVO.class, BeanUtils.toBean(list, HcWorkCenterRespVO.class));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

}
