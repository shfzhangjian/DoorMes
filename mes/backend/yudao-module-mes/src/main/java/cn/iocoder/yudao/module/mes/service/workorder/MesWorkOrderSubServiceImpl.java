package cn.iocoder.yudao.module.mes.service.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;


@Service
@Validated
public class MesWorkOrderSubServiceImpl implements MesWorkOrderSubService {

    @Resource
    private MesWorkOrderSubMapper mesWorkOrderSubMapper;

    @Override
    public Long createWorkOrderSub(MesWorkOrderSubSaveReqVO createReqVO) {
        MesWorkOrderSubDO subDO = BeanUtils.toBean(createReqVO, MesWorkOrderSubDO.class);

        // 1. 初始化状态
        if (subDO.getStatus() == null) {
            subDO.setStatus("PENDING");
        }

        // 2. 初始化统计数据 (避免 null 指针)
        subDO.setActualQty(BigDecimal.ZERO);
        subDO.setGoodQty(BigDecimal.ZERO);
        subDO.setScrapQty(BigDecimal.ZERO); // 修正: 对应数据库 scrap_qty
        subDO.setSampleQty(BigDecimal.ZERO);

        mesWorkOrderSubMapper.insert(subDO);
        return subDO.getId();
    }

    @Override
    public void updateWorkOrderSub(MesWorkOrderSubSaveReqVO updateReqVO) {
        // 1. 校验存在性
        MesWorkOrderSubDO existDO = validateWorkOrderSubExists(updateReqVO.getId());

        // 2. 架构师红线：生产中或已完工的任务，禁止随意修改核心工艺数据
        // (防止更改工序或数量导致现场执行混乱)
        if ("DOING".equals(existDO.getStatus()) || "DONE".equals(existDO.getStatus())) {
            throw new RuntimeException("当前状态(DOING/DONE)下的派工单严禁修改核心数据！");
        }

        // 3. 更新
        MesWorkOrderSubDO updateObj = BeanUtils.toBean(updateReqVO, MesWorkOrderSubDO.class);
        mesWorkOrderSubMapper.updateById(updateObj);
    }

    @Override
    public void deleteWorkOrderSub(Long id) {
        // 1. 校验存在性
        MesWorkOrderSubDO existDO = validateWorkOrderSubExists(id);

        // 2. 校验状态 (仅 PENDING 可删)
        if (!"PENDING".equals(existDO.getStatus())) {
            throw new RuntimeException("仅允许删除待接单(PENDING)状态的派工任务！当前状态: " + existDO.getStatus());
        }

        // 3. 删除
        mesWorkOrderSubMapper.deleteById(id);
    }

    @Override
    public MesWorkOrderSubDO getWorkOrderSub(Long id) {
        return mesWorkOrderSubMapper.selectById(id);
    }

    @Override
    public PageResult<MesWorkOrderSubDO> getWorkOrderSubPage(MesWorkOrderSubPageReqVO pageReqVO) {
        return mesWorkOrderSubMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<MesWorkOrderSubDO>()
                .eqIfPresent(MesWorkOrderSubDO::getWorkOrderId, pageReqVO.getWorkOrderId())
                .likeIfPresent(MesWorkOrderSubDO::getWorkOrderNo, pageReqVO.getWorkOrderNo()) // 支持主单号模糊查询
                .likeIfPresent(MesWorkOrderSubDO::getSubOrderNo, pageReqVO.getSubOrderNo())
                .likeIfPresent(MesWorkOrderSubDO::getProcessName, pageReqVO.getProcessName()) // 支持工序名称查询
                .eqIfPresent(MesWorkOrderSubDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(MesWorkOrderSubDO::getStationId, pageReqVO.getStationId())
                .likeIfPresent(MesWorkOrderSubDO::getStationName, pageReqVO.getStationName()) // 支持工位名称查询
                .eqIfPresent(MesWorkOrderSubDO::getOperatorUser, pageReqVO.getOperatorUser())
                .betweenIfPresent(MesWorkOrderSubDO::getPlanDate, pageReqVO.getPlanDate())
                .orderByAsc(MesWorkOrderSubDO::getSeqNo) // 严格按照工序先后排序
                .orderByDesc(MesWorkOrderSubDO::getId));
    }

    private MesWorkOrderSubDO validateWorkOrderSubExists(Long id) {
        MesWorkOrderSubDO subDO = mesWorkOrderSubMapper.selectById(id);
        if (subDO == null) {
            throw new RuntimeException("派工细单不存在！");
        }
        return subDO;
    }
}
