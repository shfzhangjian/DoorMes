package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackageAuxStockDO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPackageAuxStockMapper extends BaseMapperX<HcPackageAuxStockDO> {

    default PageResult<HcPackageAuxStockDO> selectPage(PackageAuxStockPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HcPackageAuxStockDO>()
                .eqIfPresent(HcPackageAuxStockDO::getAuxCategory, reqVO.getAuxCategory())
                .likeIfPresent(HcPackageAuxStockDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcPackageAuxStockDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(HcPackageAuxStockDO::getAuxSpec, reqVO.getAuxSpec())
                .likeIfPresent(HcPackageAuxStockDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcPackageAuxStockDO::getStockStatus, reqVO.getStockStatus())
                .eqIfPresent(HcPackageAuxStockDO::getReceiveDate, reqVO.getReceiveDate())
                .orderByDesc(HcPackageAuxStockDO::getReceiveDate)
                .orderByDesc(HcPackageAuxStockDO::getId));
    }

    default HcPackageAuxStockDO selectByBatchNo(String batchNo) {
        return selectOne(new LambdaQueryWrapperX<HcPackageAuxStockDO>()
                .eq(HcPackageAuxStockDO::getBatchNo, batchNo)
                .eq(HcPackageAuxStockDO::getDeleted, false)
                .orderByDesc(HcPackageAuxStockDO::getId)
                .last("LIMIT 1"));
    }

    default HcPackageAuxStockDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcPackageAuxStockDO>()
                .eq(HcPackageAuxStockDO::getId, id)
                .eq(HcPackageAuxStockDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default HcPackageAuxStockDO selectFirstAvailableByMaterialCode(String materialCode) {
        return selectOne(new LambdaQueryWrapperX<HcPackageAuxStockDO>()
                .eq(HcPackageAuxStockDO::getMaterialCode, materialCode)
                .eq(HcPackageAuxStockDO::getStockStatus, "ACTIVE")
                .gt(HcPackageAuxStockDO::getAvailableQty, BigDecimal.ZERO)
                .eq(HcPackageAuxStockDO::getDeleted, false)
                .orderByAsc(HcPackageAuxStockDO::getReceiveDate)
                .orderByAsc(HcPackageAuxStockDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcPackageAuxStockDO> selectAvailableList() {
        return selectList(new LambdaQueryWrapperX<HcPackageAuxStockDO>()
                .eq(HcPackageAuxStockDO::getStockStatus, "ACTIVE")
                .gt(HcPackageAuxStockDO::getAvailableQty, BigDecimal.ZERO)
                .eq(HcPackageAuxStockDO::getDeleted, false)
                .orderByAsc(HcPackageAuxStockDO::getAuxCategoryName)
                .orderByAsc(HcPackageAuxStockDO::getBatchNo));
    }
}
