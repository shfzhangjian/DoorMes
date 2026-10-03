package cn.iocoder.yudao.module.mes.service.hc.workcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter.HcWorkCenterDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.workcenter.HcWorkCenterMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.process.ProcessMapper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCWORKCENTER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCWORKCENTER_PROCESS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCWORKCENTER_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCWORKCENTER_WCCODE_EXISTS;

@Service
@Validated
public class HcWorkCenterServiceImpl implements HcWorkCenterService {

    @Resource
    private HcWorkCenterMapper hcWorkCenterMapper;
    @Resource
    private ProcessMapper processMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcWorkCenter(HcWorkCenterSaveReqVO createReqVO) {
        validateWcCodeUnique(null, createReqVO.getWcCode());
        normalizeProcessSnapshot(createReqVO);
        HcWorkCenterDO entity = BeanUtils.toBean(createReqVO, HcWorkCenterDO.class);
        hcWorkCenterMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcWorkCenter(HcWorkCenterSaveReqVO updateReqVO) {
        validateHcWorkCenterExists(updateReqVO.getId());
        validateWcCodeUnique(updateReqVO.getId(), updateReqVO.getWcCode());
        normalizeProcessSnapshot(updateReqVO);
        HcWorkCenterDO updateObj = BeanUtils.toBean(updateReqVO, HcWorkCenterDO.class);
        hcWorkCenterMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcWorkCenter(Long id) {
        validateHcWorkCenterExists(id);
        hcWorkCenterMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcWorkCenterListByIds(List<Long> ids) {
        hcWorkCenterMapper.deleteByIds(ids);
    }

    private void validateHcWorkCenterExists(Long id) {
        if (hcWorkCenterMapper.selectById(id) == null) {
            throw exception(HCWORKCENTER_NOT_EXISTS);
        }
    }

    private void validateWcCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcWorkCenterDO entity = hcWorkCenterMapper.selectOne(new LambdaQueryWrapperX<HcWorkCenterDO>().eq(HcWorkCenterDO::getWcCode, value).neIfPresent(HcWorkCenterDO::getId, id));
        if (entity != null) {
            throw exception(HCWORKCENTER_WCCODE_EXISTS);
        }
    }

    private void normalizeProcessSnapshot(HcWorkCenterSaveReqVO reqVO) {
        if (reqVO.getProcessId() == null) {
            throw exception(HCWORKCENTER_PROCESS_REQUIRED);
        }
        ProcessDO process = processMapper.selectById(reqVO.getProcessId());
        if (process == null || (process.getStatus() != null && process.getStatus() != 0)) {
            throw exception(HCWORKCENTER_PROCESS_INVALID);
        }
        reqVO.setProcessCode(process.getCode());
        reqVO.setProcessName(process.getName());
        reqVO.setProcessStage(process.getName());
    }

    @Override
    public HcWorkCenterDO getHcWorkCenter(Long id) {
        return hcWorkCenterMapper.selectById(id);
    }

    @Override
    public HcWorkCenterDO getHcWorkCenterByTerminalIp(String ip) {
        String normalizedIp = normalizeTerminalIp(ip);
        if (normalizedIp == null) {
            return null;
        }
        List<HcWorkCenterDO> list = getHcWorkCenterSimpleList();
        for (HcWorkCenterDO item : list) {
            if (containsTerminalIp(item.getTerminalIps(), normalizedIp)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<HcWorkCenterDO> getHcWorkCenterSimpleList() {
        LambdaQueryWrapperX<HcWorkCenterDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcWorkCenterDO::getStatus, 0);
        queryWrapper.orderByAsc(HcWorkCenterDO::getLineSort);
        queryWrapper.orderByAsc(HcWorkCenterDO::getLineCode);
        queryWrapper.orderByAsc(HcWorkCenterDO::getWcCode);
        queryWrapper.orderByDesc(HcWorkCenterDO::getId);
        return hcWorkCenterMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcWorkCenterDO> getHcWorkCenterList(HcWorkCenterPageReqVO reqVO) {
        return hcWorkCenterMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcWorkCenterDO> getHcWorkCenterPage(HcWorkCenterPageReqVO pageReqVO) {
        return hcWorkCenterMapper.selectPage(pageReqVO);
    }

    private boolean containsTerminalIp(String terminalIps, String ip) {
        if (terminalIps == null || terminalIps.isBlank()) {
            return false;
        }
        return Arrays.stream(terminalIps.split("[,，;；\\s]+"))
                .map(this::normalizeTerminalIp)
                .filter(Objects::nonNull)
                .anyMatch(ip::equals);
    }

    private String normalizeTerminalIp(String ip) {
        if (ip == null) {
            return null;
        }
        String value = ip.trim();
        return value.isEmpty() ? null : value;
    }

}
