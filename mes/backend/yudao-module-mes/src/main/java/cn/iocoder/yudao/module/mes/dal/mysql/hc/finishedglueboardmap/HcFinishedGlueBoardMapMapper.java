package cn.iocoder.yudao.module.mes.dal.mysql.hc.finishedglueboardmap;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFinishedGlueBoardMapMapper extends BaseMapperX<HcFinishedGlueBoardMapDO> {

    default PageResult<HcFinishedGlueBoardMapDO> selectPage(HcFinishedGlueBoardMapPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcFinishedGlueBoardMapDO> selectList(HcFinishedGlueBoardMapPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default List<HcFinishedGlueBoardMapDO> selectEnabledListByProductModelCode(String productModelCode) {
        return selectList(new LambdaQueryWrapperX<HcFinishedGlueBoardMapDO>()
                .eq(HcFinishedGlueBoardMapDO::getProductModelCode, productModelCode)
                .eq(HcFinishedGlueBoardMapDO::getStatus, "ENABLE")
                .orderByAsc(HcFinishedGlueBoardMapDO::getProductModelCode)
                .orderByAsc(HcFinishedGlueBoardMapDO::getProductSpec)
                .orderByDesc(HcFinishedGlueBoardMapDO::getId));
    }

    default LambdaQueryWrapperX<HcFinishedGlueBoardMapDO> buildQuery(HcFinishedGlueBoardMapPageReqVO reqVO) {
        LambdaQueryWrapperX<HcFinishedGlueBoardMapDO> query = new LambdaQueryWrapperX<HcFinishedGlueBoardMapDO>()
                .likeIfPresent(HcFinishedGlueBoardMapDO::getProductModelCode, reqVO.getProductModelCode())
                .likeIfPresent(HcFinishedGlueBoardMapDO::getProductModelName, reqVO.getProductModelName())
                .eqIfPresent(HcFinishedGlueBoardMapDO::getProductSpec, reqVO.getProductSpec())
                .eqIfPresent(HcFinishedGlueBoardMapDO::getStatus, reqVO.getStatus());
        if (StrUtil.isNotBlank(reqVO.getGlueBoardKeyword())) {
            query.and(wrapper -> wrapper
                    .like(HcFinishedGlueBoardMapDO::getAdhesive1Summary, reqVO.getGlueBoardKeyword())
                    .or()
                    .like(HcFinishedGlueBoardMapDO::getAdhesive2Summary, reqVO.getGlueBoardKeyword()));
        }
        query.orderByAsc(HcFinishedGlueBoardMapDO::getProductModelCode);
        query.orderByAsc(HcFinishedGlueBoardMapDO::getProductSpec);
        query.orderByDesc(HcFinishedGlueBoardMapDO::getId);
        return query;
    }

}
