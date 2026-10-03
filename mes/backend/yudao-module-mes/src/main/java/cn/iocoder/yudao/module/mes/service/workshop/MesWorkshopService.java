package cn.iocoder.yudao.module.mes.service.workshop;

import java.util.*;

import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workshop.MesWorkshopDO;
import jakarta.validation.*;

/**
 * MES车间产线定义 Service 接口
 *
 * @author 演示管理员
 */
public interface MesWorkshopService {

    /**
     * 创建MES车间产线定义
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMesWorkshop(@Valid MesWorkshopSaveReqVO createReqVO);

    /**
     * 更新MES车间产线定义
     *
     * @param updateReqVO 更新信息
     */
    void updateMesWorkshop(@Valid MesWorkshopSaveReqVO updateReqVO);

    /**
     * 删除MES车间产线定义
     *
     * @param id 编号
     */
    void deleteMesWorkshop(Long id);


    /**
     * 获得MES车间产线定义
     *
     * @param id 编号
     * @return MES车间产线定义
     */
    MesWorkshopDO getMesWorkshop(Long id);

    /**
     * 获得MES车间产线定义列表
     *
     * @param listReqVO 查询条件
     * @return MES车间产线定义列表
     */
    List<MesWorkshopDO> getMesWorkshopList(MesWorkshopListReqVO listReqVO);

}
