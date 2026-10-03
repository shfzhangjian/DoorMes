package cn.iocoder.yudao.module.mes.service.hc.processparam;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processparam.HcProcessParamRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processparam.HcProcessParamRecordMapper;
import cn.iocoder.yudao.module.mes.service.hc.processparam.dto.HcProcessParamRecordUpsertReq;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcProcessParamRecordServiceImpl implements HcProcessParamRecordService {

    @Resource
    private HcProcessParamRecordMapper hcProcessParamRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsertProcessParam(HcProcessParamRecordUpsertReq req) {
        validateUpsertReq(req);
        HcProcessParamRecordDO existed = hcProcessParamRecordMapper.selectOneByUnique(
                req.getTenantId(), req.getBatchNo(), req.getProcessCode(), req.getParamCode());
        if (existed == null) {
            HcProcessParamRecordDO entity = buildEntity(req);
            hcProcessParamRecordMapper.insert(entity);
            return entity.getId();
        }
        fillEntity(existed, req);
        hcProcessParamRecordMapper.updateById(existed);
        return existed.getId();
    }

    @Override
    public List<HcProcessParamRecordDO> listByBatchNosAndProcessCodes(Collection<String> batchNos,
                                                                      Collection<String> processCodes,
                                                                      String paramCode) {
        return hcProcessParamRecordMapper.selectListByBatchNosAndProcessCodes(batchNos, processCodes, paramCode);
    }

    private void validateUpsertReq(HcProcessParamRecordUpsertReq req) {
        if (req == null) {
            throw invalidParamException("工艺参数记录不能为空");
        }
        if (req.getTenantId() == null) {
            throw invalidParamException("工艺参数记录缺少租户");
        }
        if (StrUtil.isBlank(req.getBatchNo())) {
            throw invalidParamException("工艺参数记录缺少批次号");
        }
        if (StrUtil.isBlank(req.getProcessCode())) {
            throw invalidParamException("工艺参数记录缺少工序");
        }
        if (StrUtil.isBlank(req.getParamCode())) {
            throw invalidParamException("工艺参数记录缺少参数项");
        }
    }

    private HcProcessParamRecordDO buildEntity(HcProcessParamRecordUpsertReq req) {
        HcProcessParamRecordDO entity = new HcProcessParamRecordDO();
        entity.setTenantId(req.getTenantId());
        entity.setBatchNo(StrUtil.trim(req.getBatchNo()));
        entity.setProcessCode(StrUtil.trim(req.getProcessCode()));
        entity.setParamCode(StrUtil.trim(req.getParamCode()));
        fillEntity(entity, req);
        return entity;
    }

    private void fillEntity(HcProcessParamRecordDO entity, HcProcessParamRecordUpsertReq req) {
        entity.setPlanId(req.getPlanId());
        entity.setPlanNo(StrUtil.trimToNull(req.getPlanNo()));
        entity.setPlanOperationId(req.getPlanOperationId());
        entity.setBatchNo(StrUtil.trim(req.getBatchNo()));
        entity.setProcessCode(StrUtil.trim(req.getProcessCode()));
        entity.setProcessName(StrUtil.trimToNull(req.getProcessName()));
        entity.setParamCode(StrUtil.trim(req.getParamCode()));
        entity.setParamName(StrUtil.blankToDefault(StrUtil.trimToNull(req.getParamName()), req.getParamCode()));
        entity.setParamValue(StrUtil.trimToNull(req.getParamValue()));
        entity.setParamValueNum(req.getParamValueNum());
        entity.setUom(StrUtil.trimToNull(req.getUom()));
        entity.setRecordTime(req.getRecordTime() == null ? LocalDateTime.now() : req.getRecordTime());
        entity.setRecorderId(req.getRecorderId());
        entity.setRecorderName(StrUtil.trimToNull(req.getRecorderName()));
        entity.setSourceType(StrUtil.trimToNull(req.getSourceType()));
        entity.setSourceTable(StrUtil.trimToNull(req.getSourceTable()));
        entity.setSourceId(req.getSourceId());
        entity.setSourceDetailId(req.getSourceDetailId());
        entity.setSourceFormCode(StrUtil.trimToNull(req.getSourceFormCode()));
        entity.setSourceFormName(StrUtil.trimToNull(req.getSourceFormName()));
        entity.setRemark(StrUtil.trimToNull(req.getRemark()));
    }

}
