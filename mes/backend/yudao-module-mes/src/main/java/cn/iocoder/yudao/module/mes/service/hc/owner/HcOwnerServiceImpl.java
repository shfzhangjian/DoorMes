package cn.iocoder.yudao.module.mes.service.hc.owner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.owner.HcOwnerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.owner.HcOwnerMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOWNER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCOWNER_OWNERCODE_EXISTS;

@Service
@Validated
public class HcOwnerServiceImpl implements HcOwnerService {

    @Resource
    private HcOwnerMapper hcOwnerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcOwner(HcOwnerSaveReqVO createReqVO) {
        validateOwnerCodeUnique(null, createReqVO.getOwnerCode());
        HcOwnerDO entity = BeanUtils.toBean(createReqVO, HcOwnerDO.class);
        hcOwnerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcOwner(HcOwnerSaveReqVO updateReqVO) {
        validateHcOwnerExists(updateReqVO.getId());
        validateOwnerCodeUnique(updateReqVO.getId(), updateReqVO.getOwnerCode());
        HcOwnerDO updateObj = BeanUtils.toBean(updateReqVO, HcOwnerDO.class);
        hcOwnerMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcOwner(Long id) {
        validateHcOwnerExists(id);
        hcOwnerMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcOwnerListByIds(List<Long> ids) {
        hcOwnerMapper.deleteByIds(ids);
    }

    private void validateHcOwnerExists(Long id) {
        if (hcOwnerMapper.selectById(id) == null) {
            throw exception(HCOWNER_NOT_EXISTS);
        }
    }

    private void validateOwnerCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcOwnerDO entity = hcOwnerMapper.selectOne(new LambdaQueryWrapperX<HcOwnerDO>().eq(HcOwnerDO::getOwnerCode, value).neIfPresent(HcOwnerDO::getId, id));
        if (entity != null) {
            throw exception(HCOWNER_OWNERCODE_EXISTS);
        }
    }

    @Override
    public HcOwnerDO getHcOwner(Long id) {
        return hcOwnerMapper.selectById(id);
    }

    @Override
    public List<HcOwnerDO> getHcOwnerSimpleList() {
        LambdaQueryWrapperX<HcOwnerDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcOwnerDO::getStatus, "启用");
        queryWrapper.orderByAsc(HcOwnerDO::getOwnerCode);
        queryWrapper.orderByDesc(HcOwnerDO::getId);
        return hcOwnerMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcOwnerDO> getHcOwnerList(HcOwnerPageReqVO reqVO) {
        return hcOwnerMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcOwnerDO> getHcOwnerPage(HcOwnerPageReqVO pageReqVO) {
        return hcOwnerMapper.selectPage(pageReqVO);
    }

}