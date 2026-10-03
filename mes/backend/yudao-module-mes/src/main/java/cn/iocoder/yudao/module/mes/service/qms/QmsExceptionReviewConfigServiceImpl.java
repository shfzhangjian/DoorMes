package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionReviewConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionReviewConfigMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsExceptionReviewConfigServiceImpl implements QmsExceptionReviewConfigService {

    private static final ErrorCode QMS_EXCEPTION_REVIEW_CONFIG_NOT_EXISTS =
            new ErrorCode(1008100160, "异常事件临时小组配置不存在");

    @Resource
    private QmsExceptionReviewConfigMapper qmsExceptionReviewConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReviewConfig(QmsNcReviewConfigSaveReqVO createReqVO) {
        QmsExceptionReviewConfigDO entity = toDO(createReqVO);
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
        entity.setSort(entity.getSort() == null ? 0 : entity.getSort());
        qmsExceptionReviewConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewConfig(QmsNcReviewConfigSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        QmsExceptionReviewConfigDO updateObj = toDO(updateReqVO);
        updateObj.setId(updateReqVO.getId());
        qmsExceptionReviewConfigMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReviewConfig(Long id) {
        validateExists(id);
        qmsExceptionReviewConfigMapper.deleteById(id);
    }

    @Override
    public QmsNcReviewConfigRespVO getReviewConfig(Long id) {
        return toResp(validateExists(id));
    }

    @Override
    public PageResult<QmsNcReviewConfigRespVO> getReviewConfigPage(QmsNcReviewConfigPageReqVO pageReqVO) {
        PageResult<QmsExceptionReviewConfigDO> pageResult = qmsExceptionReviewConfigMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toResp).collect(Collectors.toList()),
                pageResult.getTotal());
    }

    @Override
    public List<QmsNcReviewConfigRespVO> getSimpleList() {
        List<QmsExceptionReviewConfigDO> list = qmsExceptionReviewConfigMapper.selectEnabledList();
        if (CollUtil.isEmpty(list)) {
            list = TenantUtils.executeIgnore(() -> qmsExceptionReviewConfigMapper.selectEnabledPublicList());
        }
        return list.stream()
                .map(this::toResp).collect(Collectors.toList());
    }

    private QmsExceptionReviewConfigDO validateExists(Long id) {
        QmsExceptionReviewConfigDO entity = qmsExceptionReviewConfigMapper.selectById(id);
        if (entity == null) {
            throw exception(QMS_EXCEPTION_REVIEW_CONFIG_NOT_EXISTS);
        }
        return entity;
    }

    private QmsExceptionReviewConfigDO toDO(QmsNcReviewConfigSaveReqVO reqVO) {
        QmsExceptionReviewConfigDO entity = new QmsExceptionReviewConfigDO();
        entity.setUnitCode(StrUtil.trim(reqVO.getUnitCode()));
        entity.setUnitName(StrUtil.trim(reqVO.getUnitName()));
        entity.setDeptId(reqVO.getDeptId());
        entity.setDeptName(StrUtil.trim(reqVO.getDeptName()));
        entity.setHandlerUserIds(joinLongs(reqVO.getHandlerUserIds()));
        entity.setHandlerUserNames(joinStrings(reqVO.getHandlerUserNames()));
        entity.setStatus(reqVO.getStatus());
        entity.setSort(reqVO.getSort());
        entity.setRemark(StrUtil.trim(reqVO.getRemark()));
        return entity;
    }

    private QmsNcReviewConfigRespVO toResp(QmsExceptionReviewConfigDO entity) {
        QmsNcReviewConfigRespVO respVO = new QmsNcReviewConfigRespVO();
        respVO.setId(entity.getId());
        respVO.setUnitCode(entity.getUnitCode());
        respVO.setUnitName(entity.getUnitName());
        respVO.setDeptId(entity.getDeptId());
        respVO.setDeptName(entity.getDeptName());
        respVO.setHandlerUserIds(parseLongs(entity.getHandlerUserIds()));
        respVO.setHandlerUserNames(parseStrings(entity.getHandlerUserNames()));
        respVO.setStatus(entity.getStatus());
        respVO.setSort(entity.getSort());
        respVO.setRemark(entity.getRemark());
        respVO.setCreateTime(entity.getCreateTime());
        return respVO;
    }

    private String joinLongs(List<Long> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
    }

    private String joinStrings(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.stream().filter(StrUtil::isNotBlank).map(StrUtil::trim).collect(Collectors.joining(","));
    }

    private List<Long> parseLongs(String value) {
        List<Long> result = new ArrayList<>();
        for (String item : StrUtil.splitTrim(StrUtil.blankToDefault(value, ""), ',')) {
            if (StrUtil.isNotBlank(item)) {
                result.add(Long.valueOf(item));
            }
        }
        return result;
    }

    private List<String> parseStrings(String value) {
        return StrUtil.splitTrim(StrUtil.blankToDefault(value, ""), ',').stream()
                .filter(StrUtil::isNotBlank).collect(Collectors.toList());
    }
}
