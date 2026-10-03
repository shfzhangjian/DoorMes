package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldTargetConfigDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QmsYieldTargetConfigMapper extends BaseMapperX<QmsYieldTargetConfigDO> {

    default PageResult<QmsYieldTargetConfigDO> selectPage(QmsYieldTargetConfigPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByAsc(QmsYieldTargetConfigDO::getModelCode)
                .orderByAsc(QmsYieldTargetConfigDO::getSegmentCount)
                .orderByAsc(QmsYieldTargetConfigDO::getSort)
                .orderByAsc(QmsYieldTargetConfigDO::getProcessCode)
                .orderByDesc(QmsYieldTargetConfigDO::getId));
    }

    default List<QmsYieldTargetConfigDO> selectList(QmsYieldTargetConfigPageReqVO reqVO) {
        return selectList(buildQuery(reqVO)
                .orderByAsc(QmsYieldTargetConfigDO::getModelCode)
                .orderByAsc(QmsYieldTargetConfigDO::getSegmentCount)
                .orderByAsc(QmsYieldTargetConfigDO::getSort)
                .orderByAsc(QmsYieldTargetConfigDO::getProcessCode)
                .orderByDesc(QmsYieldTargetConfigDO::getId));
    }

    default List<QmsYieldTargetConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<QmsYieldTargetConfigDO>()
                .eq(QmsYieldTargetConfigDO::getStatus, 0)
                .orderByAsc(QmsYieldTargetConfigDO::getModelCode)
                .orderByAsc(QmsYieldTargetConfigDO::getSegmentCount)
                .orderByAsc(QmsYieldTargetConfigDO::getSort)
                .orderByAsc(QmsYieldTargetConfigDO::getProcessCode)
                .orderByDesc(QmsYieldTargetConfigDO::getId));
    }

    default QmsYieldTargetConfigDO selectByModelAndProcess(String modelCode, String processCode,
                                                           Integer segmentCount, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsYieldTargetConfigDO>()
                .eq(QmsYieldTargetConfigDO::getModelCode, modelCode)
                .eq(QmsYieldTargetConfigDO::getProcessCode, processCode)
                .eq(QmsYieldTargetConfigDO::getSegmentCount, segmentCount)
                .neIfPresent(QmsYieldTargetConfigDO::getId, excludeId));
    }

    @Select("""
            SELECT DISTINCT model_code
            FROM mes_qms_yield_target_config
            WHERE deleted = b'0'
              AND status = 0
              AND tenant_id = #{tenantId}
              AND model_code IS NOT NULL
              AND TRIM(model_code) <> ''
            ORDER BY model_code
            """)
    List<String> selectEnabledModelCodes(@Param("tenantId") Long tenantId);

    private LambdaQueryWrapperX<QmsYieldTargetConfigDO> buildQuery(QmsYieldTargetConfigPageReqVO reqVO) {
        return new LambdaQueryWrapperX<QmsYieldTargetConfigDO>()
                .in(QmsYieldTargetConfigDO::getProcessCode, List.of("FORMULA", "WET"))
                .likeIfPresent(QmsYieldTargetConfigDO::getModelCode, reqVO.getModelCode())
                .eqIfPresent(QmsYieldTargetConfigDO::getProcessCode, reqVO.getProcessCode())
                .eqIfPresent(QmsYieldTargetConfigDO::getSegmentCount, reqVO.getSegmentCount())
                .eqIfPresent(QmsYieldTargetConfigDO::getStatus, reqVO.getStatus());
    }
}
