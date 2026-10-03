package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyDO;
import cn.iocoder.yudao.module.mes.service.srm.SrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

@Tag(name = "管理后台 - SRM供应商基本情况调查")
@RestController
@RequestMapping("/mes/srm/survey")
@Validated
public class SrmSurveyController {

    @Resource
    private SrmService srmService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商基本情况调查")
    public CommonResult<Long> createSurvey(@Valid @RequestBody SrmSurveySaveReqVO reqVO) {
        return success(srmService.createSurvey(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商基本情况调查")
    public CommonResult<Boolean> updateSurvey(
            @Validated({Default.class, SrmSurveySaveReqVO.Update.class})
            @RequestBody SrmSurveySaveReqVO reqVO) {
        srmService.updateSurvey(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商基本情况调查")
    public CommonResult<Boolean> deleteSurvey(@RequestParam("id") Long id) {
        srmService.deleteSurvey(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商基本情况调查")
    public CommonResult<SrmSurveyRespVO> getSurvey(@RequestParam("id") Long id) {
        return success(buildResp(srmService.getSurvey(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商基本情况调查分页")
    public CommonResult<PageResult<SrmSurveyRespVO>> getSurveyPage(@Valid SrmSurveyPageReqVO reqVO) {
        PageResult<SrmSurveyDO> pageResult = srmService.getSurveyPage(reqVO);
        return success(BeanUtils.toBean(pageResult, SrmSurveyRespVO.class));
    }

    private SrmSurveyRespVO buildResp(SrmSurveyDO survey) {
        SrmSurveyRespVO respVO = BeanUtils.toBean(survey, SrmSurveyRespVO.class);
        if (respVO == null || survey == null) {
            return respVO;
        }
        respVO.setReviews(BeanUtils.toBean(srmService.getSurveyReviewList(survey.getId()), SrmSurveyRespVO.Review.class));
        return respVO;
    }

}
