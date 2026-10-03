package cn.iocoder.yudao.module.mes.service.hc.qtimeconfig;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.qtimeconfig.HcQtimeConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.qtimeconfig.HcQtimeConfigMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcQtimeConfigServiceImpl implements HcQtimeConfigService {

    @Resource
    private HcQtimeConfigMapper hcQtimeConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcQtimeConfig(HcQtimeConfigSaveReqVO createReqVO) {
        normalizeReq(createReqVO);
        validateModelPrefixUnique(null, createReqVO.getModelPrefix());
        HcQtimeConfigDO entity = BeanUtils.toBean(createReqVO, HcQtimeConfigDO.class);
        hcQtimeConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcQtimeConfig(HcQtimeConfigSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || hcQtimeConfigMapper.selectById(updateReqVO.getId()) == null) {
            throw invalidParamException("QTIME 配置不存在");
        }
        normalizeReq(updateReqVO);
        validateModelPrefixUnique(updateReqVO.getId(), updateReqVO.getModelPrefix());
        hcQtimeConfigMapper.updateById(BeanUtils.toBean(updateReqVO, HcQtimeConfigDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcQtimeConfig(Long id) {
        if (hcQtimeConfigMapper.selectById(id) == null) {
            throw invalidParamException("QTIME 配置不存在");
        }
        hcQtimeConfigMapper.deleteById(id);
    }

    @Override
    public HcQtimeConfigDO getHcQtimeConfig(Long id) {
        return hcQtimeConfigMapper.selectById(id);
    }

    @Override
    public List<HcQtimeConfigDO> getHcQtimeConfigList(HcQtimeConfigPageReqVO reqVO) {
        return hcQtimeConfigMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcQtimeConfigDO> getHcQtimeConfigPage(HcQtimeConfigPageReqVO reqVO) {
        return hcQtimeConfigMapper.selectPage(reqVO);
    }

    private void normalizeReq(HcQtimeConfigSaveReqVO reqVO) {
        reqVO.setModelPrefix(StrUtil.trimToEmpty(reqVO.getModelPrefix()).toUpperCase(Locale.ROOT));
        reqVO.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), "ENABLED").toUpperCase(Locale.ROOT));
        if (!StrUtil.equalsAny(reqVO.getStatus(), "ENABLED", "DISABLED")) {
            throw invalidParamException("QTIME 配置状态只能是 ENABLED 或 DISABLED");
        }
    }

    private void validateModelPrefixUnique(Long id, String modelPrefix) {
        HcQtimeConfigDO existed = hcQtimeConfigMapper.selectByModelPrefix(modelPrefix);
        if (existed != null && !existed.getId().equals(id)) {
            throw invalidParamException("型号前缀 %s 的 QTIME 配置已存在", modelPrefix);
        }
    }

}
