package cn.iocoder.yudao.module.mes.service.hc.printagentpackage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackageSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printagentpackage.HcPrintAgentPackageDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.printagentpackage.HcPrintAgentPackageMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRINTAGENTPACKAGE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRINTAGENTPACKAGE_PACKAGE_URL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRINTAGENTPACKAGE_VERSION_EXISTS;

@Service
@Validated
public class HcPrintAgentPackageServiceImpl implements HcPrintAgentPackageService {

    @Resource
    private HcPrintAgentPackageMapper hcPrintAgentPackageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcPrintAgentPackage(HcPrintAgentPackageSaveReqVO createReqVO) {
        validateVersionUnique(null, createReqVO.getPackageCode(), createReqVO.getVersionNo());
        validatePackageUrl(createReqVO.getPackageUrl());
        HcPrintAgentPackageDO entity = BeanUtils.toBean(createReqVO, HcPrintAgentPackageDO.class);
        normalizeDefaults(entity);
        if (Boolean.TRUE.equals(entity.getCurrentFlag())) {
            entity.setStatus("RELEASED");
            entity.setPublishTime(entity.getPublishTime() == null ? LocalDateTime.now() : entity.getPublishTime());
            hcPrintAgentPackageMapper.clearCurrentFlag(entity.getPackageCode());
        }
        hcPrintAgentPackageMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcPrintAgentPackage(HcPrintAgentPackageSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateVersionUnique(updateReqVO.getId(), updateReqVO.getPackageCode(), updateReqVO.getVersionNo());
        validatePackageUrl(updateReqVO.getPackageUrl());
        HcPrintAgentPackageDO updateObj = BeanUtils.toBean(updateReqVO, HcPrintAgentPackageDO.class);
        normalizeDefaults(updateObj);
        if (Boolean.TRUE.equals(updateObj.getCurrentFlag())) {
            updateObj.setStatus("RELEASED");
            updateObj.setPublishTime(updateObj.getPublishTime() == null ? LocalDateTime.now() : updateObj.getPublishTime());
            hcPrintAgentPackageMapper.clearCurrentFlag(updateObj.getPackageCode());
        }
        hcPrintAgentPackageMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcPrintAgentPackage(Long id) {
        validateExists(id);
        hcPrintAgentPackageMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcPrintAgentPackageListByIds(List<Long> ids) {
        for (Long id : ids) {
            deleteHcPrintAgentPackage(id);
        }
    }

    @Override
    public HcPrintAgentPackageDO getHcPrintAgentPackage(Long id) {
        return hcPrintAgentPackageMapper.selectById(id);
    }

    @Override
    public PageResult<HcPrintAgentPackageDO> getHcPrintAgentPackagePage(HcPrintAgentPackagePageReqVO pageReqVO) {
        if (StrUtil.isBlank(pageReqVO.getPackageCode())) {
            pageReqVO.setPackageCode(DEFAULT_PACKAGE_CODE);
        }
        return hcPrintAgentPackageMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishHcPrintAgentPackage(Long id) {
        HcPrintAgentPackageDO entity = validateExists(id);
        validatePackageUrl(entity.getPackageUrl());
        hcPrintAgentPackageMapper.clearCurrentFlag(entity.getPackageCode());
        hcPrintAgentPackageMapper.updateById(HcPrintAgentPackageDO.builder()
                .id(id)
                .status("RELEASED")
                .currentFlag(true)
                .publishTime(LocalDateTime.now())
                .build());
    }

    @Override
    public HcPrintAgentPackageDO getLatestReleasedPackage(String packageCode) {
        String normalizedPackageCode = StrUtil.blankToDefault(packageCode, DEFAULT_PACKAGE_CODE);
        HcPrintAgentPackageDO current = hcPrintAgentPackageMapper.selectCurrentReleased(normalizedPackageCode);
        return current != null ? current : hcPrintAgentPackageMapper.selectLatestReleased(normalizedPackageCode);
    }

    private HcPrintAgentPackageDO validateExists(Long id) {
        HcPrintAgentPackageDO entity = hcPrintAgentPackageMapper.selectById(id);
        if (entity == null) {
            throw exception(HCPRINTAGENTPACKAGE_NOT_EXISTS);
        }
        return entity;
    }

    private void validateVersionUnique(Long id, String packageCode, String versionNo) {
        HcPrintAgentPackageDO entity = hcPrintAgentPackageMapper.selectByPackageCodeAndVersionNo(packageCode, versionNo);
        if (entity != null && !entity.getId().equals(id)) {
            throw exception(HCPRINTAGENTPACKAGE_VERSION_EXISTS);
        }
    }

    private void validatePackageUrl(String packageUrl) {
        if (StrUtil.isBlank(packageUrl)) {
            throw exception(HCPRINTAGENTPACKAGE_PACKAGE_URL_REQUIRED);
        }
    }

    private void normalizeDefaults(HcPrintAgentPackageDO entity) {
        entity.setPackageCode(StrUtil.blankToDefault(entity.getPackageCode(), DEFAULT_PACKAGE_CODE));
        entity.setStatus(StrUtil.blankToDefault(entity.getStatus(), "DRAFT"));
        entity.setCurrentFlag(Boolean.TRUE.equals(entity.getCurrentFlag()));
    }

}
