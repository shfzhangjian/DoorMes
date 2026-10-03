package cn.iocoder.yudao.module.mes.service.plan;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.plan.vo.PlanCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.plan.vo.PlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.plan.vo.PlanUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.PlanDO;
import jakarta.validation.Valid;

import java.util.Collection;


/**
 * 主生产计划 Service 接口
 *
 * @author 芋道源码
 */
public interface PlanService {

    /**
     * 创建主生产计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPlan(@Valid PlanCreateReqVO createReqVO);

    /**
     * 更新主生产计划
     *
     * @param updateReqVO 更新信息
     */
    void updatePlan(@Valid PlanUpdateReqVO updateReqVO);

    /**
     * 删除主生产计划
     *
     * @param id 编号
     */
    void deletePlan(Long id);

    /**
     * 获得主生产计划
     *
     * @param id 编号
     * @return 主生产计划
     */
    PlanDO getPlan(Long id);

    /**
     * 获得主生产计划分页
     *
     * @param pageReqVO 分页查询
     * @return 主生产计划分页
     */
    PageResult<PlanDO> getPlanPage(PlanPageReqVO pageReqVO);

    // 在接口中追加方法
    void deletePlanList(Collection<Long> ids);
}
