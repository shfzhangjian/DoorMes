package cn.iocoder.yudao.module.mes.service.plan;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.plan.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.PlanDO;
import cn.iocoder.yudao.module.mes.dal.mysql.plan.PlanMapper;
import cn.iocoder.yudao.module.mes.service.saleorder.MesSaleOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.Collection;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
// 引入新定义的错误码
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@Service
@Validated
public class PlanServiceImpl implements PlanService {

    @Resource
    private PlanMapper planMapper;

    @Resource
    private MesSaleOrderService saleOrderService;

    @Override
    public Long createPlan(PlanCreateReqVO createReqVO) {
        // 1. 业务逻辑校验
        if ("SALE".equals(createReqVO.getFromSource())) {
            if (createReqVO.getSourceId() == null) {
                throw exception(new ErrorCode(1_008_011_004, "按单生产必须选择销售订单"));
            }
            if (saleOrderService.getSaleOrder(createReqVO.getSourceId()) == null) {
                throw exception(MES_SALE_ORDER_NOT_EXISTS);
            }
        } else if ("STOCK".equals(createReqVO.getFromSource())) {
            createReqVO.setSourceId(null); // 备库模式清空订单ID
        }

        PlanDO plan = BeanUtils.toBean(createReqVO, PlanDO.class);
        plan.setStatus("CREATED"); // 初始状态
        planMapper.insert(plan);
        return plan.getId();
    }

    // 新增：计划下达接口
    public void releasePlan(Long id) {
        PlanDO plan = validatePlanExists(id);
        if (!"CREATED".equals(plan.getStatus())) {
            throw exception(MES_PLAN_RELEASE_ERROR);
        }
        // 更新状态为 RELEASED，允许下游创建工单
        plan.setStatus("RELEASED");
        planMapper.updateById(plan);
    }


    @Override
    public void updatePlan(PlanUpdateReqVO updateReqVO) {
        validatePlanExists(updateReqVO.getId());
        // TODO: 可以在这里增加业务校验，例如：状态为 COMPLETED 的计划不允许修改
        PlanDO updateObj = BeanUtils.toBean(updateReqVO, PlanDO.class);
        planMapper.updateById(updateObj);
    }

    @Override
    public void deletePlan(Long id) {
        validatePlanExists(id);
        // TODO: 可以在这里增加业务校验，例如：状态为 RELEASED/COMPLETED 的计划不允许删除
        planMapper.deleteById(id);
    }

    // 新增：批量删除实现
    @Override
    public void deletePlanList(Collection<Long> ids) {
        // 实际业务中，这里可能需要先查询状态，如果包含已发布的计划，则抛出异常
        planMapper.deleteBatchIds(ids);
    }

    @Override
    public PlanDO getPlan(Long id) {
        return planMapper.selectById(id);
    }

    @Override
    public PageResult<PlanDO> getPlanPage(PlanPageReqVO pageReqVO) {
        return planMapper.selectPage(pageReqVO);
    }

    private PlanDO validatePlanExists(Long id) {
        PlanDO plan = planMapper.selectById(id);
        if (plan == null) {
            throw exception(MES_PLAN_NOT_EXISTS);
        }
        return plan;
    }
}
