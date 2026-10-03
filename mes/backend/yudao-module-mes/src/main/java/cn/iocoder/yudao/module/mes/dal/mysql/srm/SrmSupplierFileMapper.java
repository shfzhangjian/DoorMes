package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFilePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface SrmSupplierFileMapper extends BaseMapperX<SrmSupplierFileDO> {

    default PageResult<SrmSupplierFileDO> selectPage(SrmSupplierFilePageReqVO reqVO) {
        LambdaQueryWrapperX<SrmSupplierFileDO> wrapper = new LambdaQueryWrapperX<SrmSupplierFileDO>()
                .eqIfPresent(SrmSupplierFileDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(SrmSupplierFileDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmSupplierFileDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmSupplierFileDO::getFileType, reqVO.getFileType())
                .likeIfPresent(SrmSupplierFileDO::getFileName, reqVO.getFileName())
                .likeIfPresent(SrmSupplierFileDO::getProvidedProduct, reqVO.getProvidedProduct())
                .likeIfPresent(SrmSupplierFileDO::getProductModel, reqVO.getProductModel())
                .likeIfPresent(SrmSupplierFileDO::getInspectionAgency, reqVO.getInspectionAgency())
                .likeIfPresent(SrmSupplierFileDO::getReportCode, reqVO.getReportCode())
                .eqIfPresent(SrmSupplierFileDO::getFileStatus, reqVO.getFileStatus())
                .eqIfPresent(SrmSupplierFileDO::getStandardCompliant, reqVO.getStandardCompliant())
                .betweenIfPresent(SrmSupplierFileDO::getExpiryDate, reqVO.getExpiryDate())
                .betweenIfPresent(SrmSupplierFileDO::getCreateTime, reqVO.getCreateTime());
        if (StrUtil.isNotBlank(reqVO.getSupplierInfo())) {
            wrapper.and(query -> query.like(SrmSupplierFileDO::getSupplierName, reqVO.getSupplierInfo())
                    .or()
                    .like(SrmSupplierFileDO::getSupplierCode, reqVO.getSupplierInfo()));
        }
        if ("EXPIRED".equals(reqVO.getExpiryStatus())) {
            wrapper.lt(SrmSupplierFileDO::getExpiryDate, LocalDate.now());
        } else if ("UNEXPIRED".equals(reqVO.getExpiryStatus())) {
            wrapper.and(query -> query.isNull(SrmSupplierFileDO::getExpiryDate)
                    .or()
                    .ge(SrmSupplierFileDO::getExpiryDate, LocalDate.now()));
        }
        return selectPage(reqVO, wrapper.orderByAsc(SrmSupplierFileDO::getExpiryDate)
                .orderByDesc(SrmSupplierFileDO::getId));
    }

}
