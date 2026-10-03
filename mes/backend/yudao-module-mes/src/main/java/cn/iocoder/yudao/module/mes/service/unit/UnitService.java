package cn.iocoder.yudao.module.mes.service.unit;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.mes.controller.admin.unit.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.unit.UnitDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * MES计量单位 Service 接口
 *
 * @author 演示管理员
 */
public interface UnitService {

    /**
     * 创建MES计量单位
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createUnit(@Valid UnitSaveReqVO createReqVO);

    /**
     * 更新MES计量单位
     *
     * @param updateReqVO 更新信息
     */
    void updateUnit(@Valid UnitSaveReqVO updateReqVO);

    /**
     * 删除MES计量单位
     *
     * @param id 编号
     */
    void deleteUnit(Long id);

    /**
    * 批量删除MES计量单位
    *
    * @param ids 编号
    */
    void deleteUnitListByIds(List<Long> ids);

    /**
     * 获得MES计量单位
     *
     * @param id 编号
     * @return MES计量单位
     */
    UnitDO getUnit(Long id);

    /**
     * 获得MES计量单位分页
     *
     * @param pageReqVO 分页查询
     * @return MES计量单位分页
     */
    PageResult<UnitDO> getUnitPage(UnitPageReqVO pageReqVO);

}
