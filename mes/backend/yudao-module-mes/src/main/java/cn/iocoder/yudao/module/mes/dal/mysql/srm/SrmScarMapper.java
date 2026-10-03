package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmScarDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmScarMapper extends BaseMapperX<SrmScarDO> {

    default PageResult<SrmScarDO> selectPage(SrmScarPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmScarDO>()
                .eqIfPresent(SrmScarDO::getScarNo, reqVO.getScarNo())
                .likeIfPresent(SrmScarDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmScarDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SrmScarDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(SrmScarDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmScarDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(SrmScarDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmScarDO::getIssueDate, reqVO.getIssueDate())
                .betweenIfPresent(SrmScarDO::getReplyDate, reqVO.getReplyDate())
                .betweenIfPresent(SrmScarDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmScarDO::getIssueDate)
                .orderByDesc(SrmScarDO::getId));
    }

    default SrmScarDO selectByScarNo(String scarNo) {
        return selectOne(SrmScarDO::getScarNo, scarNo);
    }

}
