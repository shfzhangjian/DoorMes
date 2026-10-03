// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.tooling.ToolingLedgerServiceImpl.java
package cn.iocoder.yudao.module.mes.service.tooling;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.tooling.vo.ToolingLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.tooling.vo.ToolingLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.tooling.ToolingLedgerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.tooling.ToolingLedgerMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

@Service
@Validated
public class ToolingLedgerServiceImpl implements ToolingLedgerService {

    @Resource
    private ToolingLedgerMapper toolingLedgerMapper;

    @Override
    public Long createToolingLedger(ToolingLedgerSaveReqVO createReqVO) {
        ToolingLedgerDO toolingDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, ToolingLedgerDO.class);
        if (toolingDO.getUsedLifeTimes() == null) {
            toolingDO.setUsedLifeTimes(0);
        }
        if (toolingDO.getStatus() == null) {
            toolingDO.setStatus("IDLE");
        }
        toolingLedgerMapper.insert(toolingDO);
        return toolingDO.getId();
    }

    @Override
    public void updateToolingLedger(ToolingLedgerSaveReqVO updateReqVO) {
        if (toolingLedgerMapper.selectById(updateReqVO.getId()) == null) {
            throw new RuntimeException("该工装治具不存在！");
        }
        ToolingLedgerDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, ToolingLedgerDO.class);
        toolingLedgerMapper.updateById(updateObj);
    }

    @Override
    public void deleteToolingLedger(Long id) {
        toolingLedgerMapper.deleteById(id);
    }

    @Override
    public ToolingLedgerDO getToolingLedger(Long id) {
        return toolingLedgerMapper.selectById(id);
    }

    @Override
    public PageResult<ToolingLedgerDO> getToolingLedgerPage(ToolingLedgerPageReqVO pageReqVO) {
        return toolingLedgerMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<ToolingLedgerDO>()
                .likeIfPresent(ToolingLedgerDO::getToolingCode, pageReqVO.getToolingCode())
                .likeIfPresent(ToolingLedgerDO::getToolingName, pageReqVO.getToolingName())
                .eqIfPresent(ToolingLedgerDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(ToolingLedgerDO::getId));
    }

    @Override
    public void validateToolingLife(String toolingCode) {
        ToolingLedgerDO tooling = toolingLedgerMapper.selectOne(ToolingLedgerDO::getToolingCode, toolingCode);
        if (tooling != null) {
            if ("SCRAP".equals(tooling.getStatus())) {
                throw new RuntimeException("工装治具[" + toolingCode + "]已报废，禁止使用！");
            }
            if (tooling.getMaxLifeTimes() != null && tooling.getUsedLifeTimes() >= tooling.getMaxLifeTimes()) {
                throw new RuntimeException("工装治具[" + toolingCode + "]已达最大寿命极限，请立刻安排修磨或更换！");
            }
        }
    }
}
