package cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.qtimeconfig.HcQtimeConfigDO;
import cn.iocoder.yudao.module.mes.service.hc.qtimeconfig.HcQtimeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - HC 额定 QTIME 配置")
@RestController
@RequestMapping("/mes/hc/base/qtime-config")
@Validated
public class HcQtimeConfigController {

    @Resource
    private HcQtimeConfigService hcQtimeConfigService;

    @PostMapping("/create")
    @Operation(summary = "新增 HC 额定 QTIME 配置")
    public CommonResult<Long> createHcQtimeConfig(@Valid @RequestBody HcQtimeConfigSaveReqVO createReqVO) {
        return success(hcQtimeConfigService.createHcQtimeConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 HC 额定 QTIME 配置")
    public CommonResult<Boolean> updateHcQtimeConfig(@Valid @RequestBody HcQtimeConfigSaveReqVO updateReqVO) {
        hcQtimeConfigService.updateHcQtimeConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 HC 额定 QTIME 配置")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<Boolean> deleteHcQtimeConfig(@RequestParam("id") Long id) {
        hcQtimeConfigService.deleteHcQtimeConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取 HC 额定 QTIME 配置")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<HcQtimeConfigRespVO> getHcQtimeConfig(@RequestParam("id") Long id) {
        HcQtimeConfigDO entity = hcQtimeConfigService.getHcQtimeConfig(id);
        return success(BeanUtils.toBean(entity, HcQtimeConfigRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取 HC 额定 QTIME 配置列表")
    public CommonResult<List<HcQtimeConfigRespVO>> getHcQtimeConfigList(@Valid HcQtimeConfigPageReqVO reqVO) {
        List<HcQtimeConfigDO> list = hcQtimeConfigService.getHcQtimeConfigList(reqVO);
        return success(BeanUtils.toBean(list, HcQtimeConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取 HC 额定 QTIME 配置分页")
    public CommonResult<PageResult<HcQtimeConfigRespVO>> getHcQtimeConfigPage(@Valid HcQtimeConfigPageReqVO reqVO) {
        PageResult<HcQtimeConfigDO> pageResult = hcQtimeConfigService.getHcQtimeConfigPage(reqVO);
        return success(BeanUtils.toBean(pageResult, HcQtimeConfigRespVO.class));
    }

}
