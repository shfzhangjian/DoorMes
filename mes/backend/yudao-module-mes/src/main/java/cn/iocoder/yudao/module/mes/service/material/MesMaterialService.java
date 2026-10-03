package cn.iocoder.yudao.module.mes.service.material;

import java.util.*;

import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import jakarta.validation.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * MES物料主数据 Service 接口
 *
 * @author 演示管理员
 */
public interface MesMaterialService {

    /**
     * 创建MES物料主数据
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMesMaterial(@Valid MesMaterialSaveReqVO createReqVO);

    /**
     * 更新MES物料主数据
     *
     * @param updateReqVO 更新信息
     */
    void updateMesMaterial(@Valid MesMaterialSaveReqVO updateReqVO);

    /**
     * 删除MES物料主数据
     *
     * @param id 编号
     */
    void deleteMesMaterial(Long id);

    /**
    * 批量删除MES物料主数据
    *
    * @param ids 编号
    */
    void deleteMesMaterialListByIds(List<Long> ids);

    /**
     * 获得MES物料主数据
     *
     * @param id 编号
     * @return MES物料主数据
     */
    MesMaterialDO getMesMaterial(Long id);

    /**
     * 获得MES物料主数据分页
     *
     * @param pageReqVO 分页查询
     * @return MES物料主数据分页
     */
    PageResult<MesMaterialDO> getMesMaterialPage(MesMaterialPageReqVO pageReqVO);

}
