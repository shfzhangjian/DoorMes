package cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.service.hc.stationrecord.HcStationRecordQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工位记录查询")
@RestController
@RequestMapping("/mes/hc/plan/station-record-query")
@Validated
public class HcStationRecordQueryController {

    @Resource
    private HcStationRecordQueryService hcStationRecordQueryService;

    @GetMapping("/page")
    @Operation(summary = "工位记录分页查询")
    public CommonResult<PageResult<HcStationRecordRespVO>> getStationRecordPage(@Valid HcStationRecordPageReqVO reqVO) {
        return success(hcStationRecordQueryService.getStationRecordPage(reqVO));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "工位记录详情")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<HcStationRecordDetailRespVO> getStationRecordDetail(@RequestParam("id") Long id) {
        return success(hcStationRecordQueryService.getStationRecordDetail(id));
    }

    @PutMapping("/update")
    @Operation(summary = "编辑工位记录详情")
    public CommonResult<Boolean> updateStationRecord(@Valid @RequestBody HcStationRecordSaveReqVO reqVO) {
        hcStationRecordQueryService.updateStationRecord(reqVO);
        return success(true);
    }
}
