package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMsaRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMsaRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolMsaRecordDO;
import cn.iocoder.yudao.module.mes.service.qms.measuretool.QmsMeasureToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 量检具MSA分析历史")
@RestController
@RequestMapping("/mes/quality/measure-tool/msa-record")
@Validated
public class QmsMeasureToolMsaRecordController {

    @Resource
    private QmsMeasureToolService measureToolService;

    @GetMapping("/page")
    @Operation(summary = "获得量检具MSA分析历史分页")
    public CommonResult<PageResult<QmsMeasureToolMsaRecordRespVO>> getPage(
            @Valid QmsMeasureToolMsaRecordPageReqVO reqVO) {
        PageResult<QmsMeasureToolMsaRecordDO> pageResult = measureToolService.getMsaRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, QmsMeasureToolMsaRecordRespVO.class));
    }
}
