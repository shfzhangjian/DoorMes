package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmDocumentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmDocumentMapper extends BaseMapperX<SrmDocumentDO> {

    default PageResult<SrmDocumentDO> selectPage(SrmDocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmDocumentDO>()
                .eqIfPresent(SrmDocumentDO::getBizType, reqVO.getBizType())
                .eqIfPresent(SrmDocumentDO::getDocNo, reqVO.getDocNo())
                .likeIfPresent(SrmDocumentDO::getTitle, reqVO.getTitle())
                .likeIfPresent(SrmDocumentDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SrmDocumentDO::getMaterialName, reqVO.getMaterialName())
                .eqIfPresent(SrmDocumentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SrmDocumentDO::getBizCategory, reqVO.getBizCategory())
                .eqIfPresent(SrmDocumentDO::getBizLevel, reqVO.getBizLevel())
                .eqIfPresent(SrmDocumentDO::getPeriodType, reqVO.getPeriodType())
                .eqIfPresent(SrmDocumentDO::getEvalYear, reqVO.getEvalYear())
                .eqIfPresent(SrmDocumentDO::getEvalQuarter, reqVO.getEvalQuarter())
                .betweenIfPresent(SrmDocumentDO::getDueDate, reqVO.getDueDate())
                .betweenIfPresent(SrmDocumentDO::getApplyTime, reqVO.getApplyTime())
                .betweenIfPresent(SrmDocumentDO::getCreateTime, reqVO.getCreateTime())
                .like(StrUtil.isNotBlank(reqVO.getPayloadKeyword()), SrmDocumentDO::getPayloadJson, reqVO.getPayloadKeyword())
                .orderByDesc(SrmDocumentDO::getApplyTime)
                .orderByDesc(SrmDocumentDO::getId));
    }

    default SrmDocumentDO selectByBizTypeAndDocNo(String bizType, String docNo) {
        return selectOne(new LambdaQueryWrapperX<SrmDocumentDO>()
                .eq(SrmDocumentDO::getBizType, bizType)
                .eq(SrmDocumentDO::getDocNo, docNo));
    }

}
