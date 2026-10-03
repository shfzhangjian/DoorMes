package cn.iocoder.yudao.module.mes.service.hc.location;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcLocationMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCLOCATION_LOCATIONCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCLOCATION_NOT_EXISTS;

@Service
@Validated
public class HcLocationServiceImpl implements HcLocationService {

    private static final String PACKAGE_FG_SCENE = "PACKAGE_FG";

    @Resource
    private HcLocationMapper hcLocationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcLocation(HcLocationSaveReqVO createReqVO) {
        validateLocationCodeUnique(null, createReqVO.getLocationCode());
        HcLocationDO entity = BeanUtils.toBean(createReqVO, HcLocationDO.class);
        hcLocationMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcLocation(HcLocationSaveReqVO updateReqVO) {
        validateGeneralLocation(updateReqVO.getId());
        validateLocationCodeUnique(updateReqVO.getId(), updateReqVO.getLocationCode());
        HcLocationDO updateObj = BeanUtils.toBean(updateReqVO, HcLocationDO.class);
        hcLocationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcLocation(Long id) {
        validateGeneralLocation(id);
        hcLocationMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcLocationListByIds(List<Long> ids) {
        for (Long id : ids) {
            validateGeneralLocation(id);
        }
        hcLocationMapper.deleteByIds(ids);
    }

    private HcLocationDO validateGeneralLocation(Long id) {
        HcLocationDO location = hcLocationMapper.selectById(id);
        if (location == null) {
            throw exception(HCLOCATION_NOT_EXISTS);
        }
        if (PACKAGE_FG_SCENE.equals(location.getBizScene())) {
            throw invalidParamException("包装成品库库位请在成品库位管理页面维护");
        }
        return location;
    }

    private void validateLocationCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcLocationDO entity = hcLocationMapper.selectOne(new LambdaQueryWrapperX<HcLocationDO>().eq(HcLocationDO::getLocationCode, value).neIfPresent(HcLocationDO::getId, id));
        if (entity != null) {
            throw exception(HCLOCATION_LOCATIONCODE_EXISTS);
        }
    }

    @Override
    public HcLocationDO getHcLocation(Long id) {
        return validateGeneralLocation(id);
    }

    @Override
    public List<HcLocationDO> getHcLocationSimpleList() {
        LambdaQueryWrapperX<HcLocationDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcLocationDO::getStatus, "启用");
        queryWrapper.and(wrapper -> wrapper.isNull(HcLocationDO::getBizScene)
                .or().ne(HcLocationDO::getBizScene, PACKAGE_FG_SCENE));
        queryWrapper.orderByAsc(HcLocationDO::getLocationCode);
        queryWrapper.orderByDesc(HcLocationDO::getId);
        return hcLocationMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcLocationDO> getHcLocationList(HcLocationPageReqVO reqVO) {
        return hcLocationMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcLocationDO> getHcLocationPage(HcLocationPageReqVO pageReqVO) {
        return hcLocationMapper.selectPage(pageReqVO);
    }

}
