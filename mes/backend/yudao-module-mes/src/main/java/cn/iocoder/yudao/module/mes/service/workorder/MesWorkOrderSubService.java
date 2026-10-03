package cn.iocoder.yudao.module.mes.service.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import jakarta.validation.Valid;

/**
 * 派工细单 Service 接口
 */
public interface MesWorkOrderSubService {

    /**
     * 创建派工细单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkOrderSub(@Valid MesWorkOrderSubSaveReqVO createReqVO);

    /**
     * 更新派工细单
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkOrderSub(@Valid MesWorkOrderSubSaveReqVO updateReqVO);

    /**
     * 删除派工细单
     *
     * @param id 编号
     */
    void deleteWorkOrderSub(Long id);

    /**
     * 获得派工细单
     *
     * @param id 编号
     * @return 派工细单
     */
    MesWorkOrderSubDO getWorkOrderSub(Long id);

    /**
     * 获得派工细单分页
     *
     * @param pageReqVO 分页查询
     * @return 派工细单分页
     */
    PageResult<MesWorkOrderSubDO> getWorkOrderSubPage(MesWorkOrderSubPageReqVO pageReqVO);

}
