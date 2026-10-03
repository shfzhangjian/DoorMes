package cn.iocoder.yudao.module.mes.service.hc.fifopolicy;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.fifopolicy.HcFifoPolicyDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.fifopolicy.HcFifoPolicyMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFIFOPOLICY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFIFOPOLICY_POLICYCODE_EXISTS;

@Service
@Validated
public class HcFifoPolicyServiceImpl implements HcFifoPolicyService {

    @Resource
    private HcFifoPolicyMapper hcFifoPolicyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcFifoPolicy(HcFifoPolicySaveReqVO createReqVO) {
        validatePolicyCodeUnique(null, createReqVO.getPolicyCode());
        HcFifoPolicyDO entity = BeanUtils.toBean(createReqVO, HcFifoPolicyDO.class);
        hcFifoPolicyMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcFifoPolicy(HcFifoPolicySaveReqVO updateReqVO) {
        validateHcFifoPolicyExists(updateReqVO.getId());
        validatePolicyCodeUnique(updateReqVO.getId(), updateReqVO.getPolicyCode());
        HcFifoPolicyDO updateObj = BeanUtils.toBean(updateReqVO, HcFifoPolicyDO.class);
        hcFifoPolicyMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcFifoPolicy(Long id) {
        validateHcFifoPolicyExists(id);
        hcFifoPolicyMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcFifoPolicyListByIds(List<Long> ids) {
        hcFifoPolicyMapper.deleteByIds(ids);
    }

    private void validateHcFifoPolicyExists(Long id) {
        if (hcFifoPolicyMapper.selectById(id) == null) {
            throw exception(HCFIFOPOLICY_NOT_EXISTS);
        }
    }

    private void validatePolicyCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcFifoPolicyDO entity = hcFifoPolicyMapper.selectOne(new LambdaQueryWrapperX<HcFifoPolicyDO>().eq(HcFifoPolicyDO::getPolicyCode, value).neIfPresent(HcFifoPolicyDO::getId, id));
        if (entity != null) {
            throw exception(HCFIFOPOLICY_POLICYCODE_EXISTS);
        }
    }

    @Override
    public HcFifoPolicyDO getHcFifoPolicy(Long id) {
        return hcFifoPolicyMapper.selectById(id);
    }

    @Override
    public List<HcFifoPolicyDO> getHcFifoPolicySimpleList() {
        LambdaQueryWrapperX<HcFifoPolicyDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcFifoPolicyDO::getStatus, "启用");
        queryWrapper.orderByAsc(HcFifoPolicyDO::getPolicyCode);
        queryWrapper.orderByDesc(HcFifoPolicyDO::getId);
        return hcFifoPolicyMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcFifoPolicyDO> getHcFifoPolicyList(HcFifoPolicyPageReqVO reqVO) {
        return hcFifoPolicyMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcFifoPolicyDO> getHcFifoPolicyPage(HcFifoPolicyPageReqVO pageReqVO) {
        return hcFifoPolicyMapper.selectPage(pageReqVO);
    }

}