package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleRequestDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleRequestMapper extends BaseMapperX<SrmSampleRequestDO> {

    default PageResult<SrmSampleRequestDO> selectPage(SrmSampleRequestPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSampleRequestDO>()
                .likeIfPresent(SrmSampleRequestDO::getRequestNo, reqVO.getRequestNo())
                .likeIfPresent(SrmSampleRequestDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmSampleRequestDO::getMaterialModel, reqVO.getMaterialModel())
                .likeIfPresent(SrmSampleRequestDO::getUsedProduct, reqVO.getUsedProduct())
                .likeIfPresent(SrmSampleRequestDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmSampleRequestDO::getApplyType, reqVO.getApplyType())
                .eqIfPresent(SrmSampleRequestDO::getApplyDept, reqVO.getApplyDept())
                .eqIfPresent(SrmSampleRequestDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmSampleRequestDO::getApplyDate, reqVO.getApplyDate())
                .betweenIfPresent(SrmSampleRequestDO::getRequireDate, reqVO.getRequireDate())
                .betweenIfPresent(SrmSampleRequestDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmSampleRequestDO::getUpdateTime)
                .orderByDesc(SrmSampleRequestDO::getId));
    }

    default SrmSampleRequestDO selectByRequestNo(String requestNo) {
        return selectOne(SrmSampleRequestDO::getRequestNo, requestNo);
    }

    default int incrementSampleEvaluationCount(Long id) {
        return update(null, new LambdaUpdateWrapper<SrmSampleRequestDO>()
                .setSql("sample_evaluation_count = IFNULL(sample_evaluation_count, 0) + 1")
                .eq(SrmSampleRequestDO::getId, id));
    }

}
