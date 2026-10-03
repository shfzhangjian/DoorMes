package cn.iocoder.yudao.module.mes.dal.mysql.hc.researchtask;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.researchtask.HcResearchTaskDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcResearchTaskMapper extends BaseMapperX<HcResearchTaskDO> {

    default PageResult<HcResearchTaskDO> selectPage(HcResearchTaskPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcResearchTaskDO> selectList(HcResearchTaskPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcResearchTaskDO> buildQuery(HcResearchTaskPageReqVO reqVO) {
        LambdaQueryWrapperX<HcResearchTaskDO> query = new LambdaQueryWrapperX<HcResearchTaskDO>()
                .likeIfPresent(HcResearchTaskDO::getTaskNo, reqVO.getTaskNo())
                .likeIfPresent(HcResearchTaskDO::getRdModelCode, reqVO.getRdModelCode())
                .inIfPresent(HcResearchTaskDO::getTaskStatus, reqVO.getTaskStatuses())
                .eqIfPresent(HcResearchTaskDO::getTaskStatus, reqVO.getTaskStatus())
                .eqIfPresent(HcResearchTaskDO::getProductClassCode, reqVO.getProductClassCode())
                .eqIfPresent(HcResearchTaskDO::getBaseFormulaCode, reqVO.getBaseFormulaCode())
                .eqIfPresent(HcResearchTaskDO::getWetProcessCode, reqVO.getWetProcessCode())
                .eqIfPresent(HcResearchTaskDO::getGrindingProcessCode, reqVO.getGrindingProcessCode())
                .eqIfPresent(HcResearchTaskDO::getPostProcessCode, reqVO.getPostProcessCode())
                .likeIfPresent(HcResearchTaskDO::getRouteCode, reqVO.getRouteCode())
                .geIfPresent(HcResearchTaskDO::getResearchDate, reqVO.getResearchDateStart())
                .leIfPresent(HcResearchTaskDO::getResearchDate, reqVO.getResearchDateEnd());
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            query.and(wrapper -> wrapper.like(HcResearchTaskDO::getTaskNo, keyword)
                    .or()
                    .like(HcResearchTaskDO::getRdModelCode, keyword)
                    .or()
                    .like(HcResearchTaskDO::getBaseFormulaCode, keyword)
                    .or()
                    .like(HcResearchTaskDO::getBaseFormulaName, keyword)
                    .or()
                    .like(HcResearchTaskDO::getRouteCode, keyword)
                    .or()
                    .like(HcResearchTaskDO::getRouteName, keyword)
                    .or()
                    .like(HcResearchTaskDO::getTaskPurpose, keyword));
        }
        return query.orderByDesc(HcResearchTaskDO::getId);
    }

    default HcResearchTaskDO selectByModelCode(String modelCode) {
        return selectOne(HcResearchTaskDO::getRdModelCode, modelCode);
    }

    default Long countByCombination(Long excludeId, String productClassCode, String formulaCode,
            String wetProcessCode, String grindingProcessCode, String postProcessCode) {
        return selectCount(new LambdaQueryWrapperX<HcResearchTaskDO>()
                .neIfPresent(HcResearchTaskDO::getId, excludeId)
                .eq(HcResearchTaskDO::getProductClassCode, productClassCode)
                .eq(HcResearchTaskDO::getBaseFormulaCode, formulaCode)
                .eq(HcResearchTaskDO::getWetProcessCode, wetProcessCode)
                .eq(HcResearchTaskDO::getGrindingProcessCode, grindingProcessCode)
                .eq(HcResearchTaskDO::getPostProcessCode, postProcessCode));
    }

}
