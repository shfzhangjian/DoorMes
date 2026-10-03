package cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintCustomerInfoDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcVisualPrintCustomerInfoMapper extends BaseMapperX<HcVisualPrintCustomerInfoDO> {

    default PageResult<HcVisualPrintCustomerInfoDO> selectPage(HcVisualPrintCustomerInfoPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcVisualPrintCustomerInfoDO> selectList(HcVisualPrintCustomerInfoPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default HcVisualPrintCustomerInfoDO selectBySourceRow(Long tenantId, Integer sourceRow) {
        return selectOne(new LambdaQueryWrapperX<HcVisualPrintCustomerInfoDO>()
                .eq(HcVisualPrintCustomerInfoDO::getTenantId, tenantId)
                .eq(HcVisualPrintCustomerInfoDO::getSourceRow, sourceRow));
    }

    default LambdaQueryWrapperX<HcVisualPrintCustomerInfoDO> buildQuery(HcVisualPrintCustomerInfoPageReqVO reqVO) {
        LambdaQueryWrapperX<HcVisualPrintCustomerInfoDO> queryWrapper = new LambdaQueryWrapperX<HcVisualPrintCustomerInfoDO>()
                .likeIfPresent(HcVisualPrintCustomerInfoDO::getCustomer, reqVO.getCustomer())
                .eqIfPresent(HcVisualPrintCustomerInfoDO::getStatus, reqVO.getStatus());
        queryWrapper.last("ORDER BY "
                + "CASE WHEN serial_no REGEXP '^[0-9]+$' THEN CAST(serial_no AS UNSIGNED) ELSE 2147483647 END ASC, "
                + "serial_no ASC, source_row ASC, id ASC");
        return queryWrapper;
    }

}
