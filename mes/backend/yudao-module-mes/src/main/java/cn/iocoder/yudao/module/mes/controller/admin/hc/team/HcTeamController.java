package cn.iocoder.yudao.module.mes.controller.admin.hc.team;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.team.HcTeamDO;
import cn.iocoder.yudao.module.mes.service.hc.team.HcTeamService;
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

@Tag(name = "???? - 班组")
@RestController
@RequestMapping("/mes/hc/base/team")
@Validated
public class HcTeamController {

    @Resource
    private HcTeamService hcTeamService;

    @PostMapping("/create")
    @Operation(summary = "??班组")
    public CommonResult<Long> createHcTeam(@Valid @RequestBody HcTeamSaveReqVO createReqVO) {
        return success(hcTeamService.createHcTeam(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??班组")
    public CommonResult<Boolean> updateHcTeam(@Valid @RequestBody HcTeamSaveReqVO updateReqVO) {
        hcTeamService.updateHcTeam(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??班组")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcTeam(@RequestParam("id") Long id) {
        hcTeamService.deleteHcTeam(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????班组")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcTeamList(@RequestParam("ids") List<Long> ids) {
        hcTeamService.deleteHcTeamListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??班组??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcTeamRespVO> getHcTeam(@RequestParam("id") Long id) {
        HcTeamDO entity = hcTeamService.getHcTeam(id);
        return success(BeanUtils.toBean(entity, HcTeamRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??班组????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcTeamDetailRespVO> getHcTeamDetail(@RequestParam("id") Long id) {
        HcTeamDO entity = hcTeamService.getHcTeam(id);
        HcTeamDetailRespVO respVO = BeanUtils.toBean(entity, HcTeamDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??班组????")
    public CommonResult<List<HcTeamSimpleRespVO>> getHcTeamSimpleList() {
        List<HcTeamDO> list = hcTeamService.getHcTeamSimpleList();
        return success(BeanUtils.toBean(list, HcTeamSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??班组????")
    public CommonResult<Map<Long, String>> getHcTeamSimpleMap() {
        List<HcTeamDO> list = hcTeamService.getHcTeamSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getTeamName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??班组????")
    public CommonResult<List<HcTeamSelectOptionRespVO>> getHcTeamSelectOptions() {
        List<HcTeamDO> list = hcTeamService.getHcTeamSimpleList();
        List<HcTeamSelectOptionRespVO> result = new ArrayList<>();
        for (HcTeamDO item : list) {
            HcTeamSelectOptionRespVO option = new HcTeamSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getTeamName());
            option.setCode(item.getTeamCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??班组??")
    public CommonResult<List<HcTeamRespVO>> getHcTeamList(@Valid HcTeamPageReqVO reqVO) {
        List<HcTeamDO> list = hcTeamService.getHcTeamList(reqVO);
        return success(BeanUtils.toBean(list, HcTeamRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??班组??")
    public CommonResult<PageResult<HcTeamRespVO>> getHcTeamPage(@Valid HcTeamPageReqVO pageReqVO) {
        PageResult<HcTeamDO> pageResult = hcTeamService.getHcTeamPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcTeamRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??班组 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcTeamExcel(@Valid HcTeamPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcTeamDO> list = hcTeamService.getHcTeamPage(pageReqVO).getList();
        ExcelUtils.write(response, "班组.xls", "??", HcTeamRespVO.class, BeanUtils.toBean(list, HcTeamRespVO.class));
    }

}