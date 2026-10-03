package cn.iocoder.yudao.module.mes.dal.mysql.hc.printagentpackage;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackagePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printagentpackage.HcPrintAgentPackageDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPrintAgentPackageMapper extends BaseMapperX<HcPrintAgentPackageDO> {

    default PageResult<HcPrintAgentPackageDO> selectPage(HcPrintAgentPackagePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default HcPrintAgentPackageDO selectByPackageCodeAndVersionNo(String packageCode, String versionNo) {
        return selectOne(new LambdaQueryWrapperX<HcPrintAgentPackageDO>()
                .eq(HcPrintAgentPackageDO::getPackageCode, packageCode)
                .eq(HcPrintAgentPackageDO::getVersionNo, versionNo));
    }

    default HcPrintAgentPackageDO selectCurrentReleased(String packageCode) {
        return selectOne(new LambdaQueryWrapperX<HcPrintAgentPackageDO>()
                .eq(HcPrintAgentPackageDO::getPackageCode, packageCode)
                .eq(HcPrintAgentPackageDO::getStatus, "RELEASED")
                .eq(HcPrintAgentPackageDO::getCurrentFlag, true)
                .orderByDesc(HcPrintAgentPackageDO::getPublishTime)
                .orderByDesc(HcPrintAgentPackageDO::getId)
                .last("LIMIT 1"));
    }

    default HcPrintAgentPackageDO selectLatestReleased(String packageCode) {
        return selectOne(new LambdaQueryWrapperX<HcPrintAgentPackageDO>()
                .eq(HcPrintAgentPackageDO::getPackageCode, packageCode)
                .eq(HcPrintAgentPackageDO::getStatus, "RELEASED")
                .orderByDesc(HcPrintAgentPackageDO::getPublishTime)
                .orderByDesc(HcPrintAgentPackageDO::getId)
                .last("LIMIT 1"));
    }

    default void clearCurrentFlag(String packageCode) {
        List<HcPrintAgentPackageDO> currentRows = selectList(new LambdaQueryWrapperX<HcPrintAgentPackageDO>()
                .eq(HcPrintAgentPackageDO::getPackageCode, packageCode)
                .eq(HcPrintAgentPackageDO::getCurrentFlag, true));
        currentRows.forEach(row -> updateById(HcPrintAgentPackageDO.builder()
                .id(row.getId())
                .currentFlag(false)
                .build()));
    }

    default LambdaQueryWrapperX<HcPrintAgentPackageDO> buildQuery(HcPrintAgentPackagePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcPrintAgentPackageDO>()
                .eqIfPresent(HcPrintAgentPackageDO::getPackageCode, reqVO.getPackageCode())
                .likeIfPresent(HcPrintAgentPackageDO::getVersionNo, reqVO.getVersionNo())
                .likeIfPresent(HcPrintAgentPackageDO::getPackageName, reqVO.getPackageName())
                .eqIfPresent(HcPrintAgentPackageDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcPrintAgentPackageDO::getCurrentFlag, reqVO.getCurrentFlag())
                .orderByDesc(HcPrintAgentPackageDO::getCurrentFlag)
                .orderByDesc(HcPrintAgentPackageDO::getPublishTime)
                .orderByDesc(HcPrintAgentPackageDO::getId);
    }

}
