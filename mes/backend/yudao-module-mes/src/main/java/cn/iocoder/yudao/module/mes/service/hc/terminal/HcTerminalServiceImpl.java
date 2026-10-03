package cn.iocoder.yudao.module.mes.service.hc.terminal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.terminal.HcTerminalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.terminal.HcTerminalMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCTERMINAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCTERMINAL_TERMINALCODE_EXISTS;

@Service
@Validated
public class HcTerminalServiceImpl implements HcTerminalService {

    @Resource
    private HcTerminalMapper hcTerminalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcTerminal(HcTerminalSaveReqVO createReqVO) {
        validateTerminalCodeUnique(null, createReqVO.getTerminalCode());
        HcTerminalDO entity = BeanUtils.toBean(createReqVO, HcTerminalDO.class);
        hcTerminalMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcTerminal(HcTerminalSaveReqVO updateReqVO) {
        validateHcTerminalExists(updateReqVO.getId());
        validateTerminalCodeUnique(updateReqVO.getId(), updateReqVO.getTerminalCode());
        HcTerminalDO updateObj = BeanUtils.toBean(updateReqVO, HcTerminalDO.class);
        hcTerminalMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcTerminal(Long id) {
        validateHcTerminalExists(id);
        hcTerminalMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcTerminalListByIds(List<Long> ids) {
        hcTerminalMapper.deleteByIds(ids);
    }

    private void validateHcTerminalExists(Long id) {
        if (hcTerminalMapper.selectById(id) == null) {
            throw exception(HCTERMINAL_NOT_EXISTS);
        }
    }

    private void validateTerminalCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcTerminalDO entity = hcTerminalMapper.selectOne(new LambdaQueryWrapperX<HcTerminalDO>().eq(HcTerminalDO::getTerminalCode, value).neIfPresent(HcTerminalDO::getId, id));
        if (entity != null) {
            throw exception(HCTERMINAL_TERMINALCODE_EXISTS);
        }
    }

    @Override
    public HcTerminalDO getHcTerminal(Long id) {
        return hcTerminalMapper.selectById(id);
    }

    @Override
    public List<HcTerminalDO> getHcTerminalSimpleList() {
        LambdaQueryWrapperX<HcTerminalDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcTerminalDO::getStatus, 0);
        queryWrapper.orderByAsc(HcTerminalDO::getTerminalCode);
        queryWrapper.orderByDesc(HcTerminalDO::getId);
        return hcTerminalMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcTerminalDO> getHcTerminalList(HcTerminalPageReqVO reqVO) {
        return hcTerminalMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcTerminalDO> getHcTerminalPage(HcTerminalPageReqVO pageReqVO) {
        return hcTerminalMapper.selectPage(pageReqVO);
    }

}