package cn.iocoder.yudao.module.mes.service.hc.paramrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord.HcParamRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord.HcSfcReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.paramrecord.HcParamRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.paramrecord.HcSfcReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workorder.MesWorkOrderMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPARAMRECORD_REPORT_NOT_EXISTS;

@Service
@Validated
public class HcParamRecordServiceImpl implements HcParamRecordService {

    @Resource
    private HcParamRecordMapper hcParamRecordMapper;

    @Resource
    private HcSfcReportMapper hcSfcReportMapper;

    @Resource
    private MesWorkOrderMapper mesWorkOrderMapper;

    @Resource
    private HcRouteMapper hcRouteMapper;

    @Resource
    private HcRouteOperationMapper hcRouteOperationMapper;

    @Override
    public HcParamRecordContextRespVO getParamRecordContext(Long reportId, String reportNo) {
        HcSfcReportDO report = getRequiredReport(reportId, reportNo);
        MesWorkOrderDO workOrder = mesWorkOrderMapper.selectById(report.getWorkOrderId());
        HcRouteDO route = workOrder == null ? null : hcRouteMapper.selectById(workOrder.getRouteId());
        HcRouteOperationDO routeOperation = route == null
                ? null
                : hcRouteOperationMapper.selectOne(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HcRouteOperationDO>()
                        .eq(HcRouteOperationDO::getRouteId, route.getId())
                        .eq(HcRouteOperationDO::getOperationCode, report.getOperationCode()));

        HcParamRecordContextRespVO respVO = new HcParamRecordContextRespVO();
        respVO.setReportId(report.getId());
        respVO.setReportNo(report.getReportNo());
        respVO.setWorkOrderId(report.getWorkOrderId());
        respVO.setWorkOrderNo(report.getWorkOrderNo());
        respVO.setOperationCode(report.getOperationCode());
        respVO.setOperationName(routeOperation != null ? routeOperation.getOperationName() : report.getOperationCode());
        if (route != null) {
            respVO.setRouteId(route.getId());
            respVO.setRouteCode(route.getRouteCode());
            respVO.setRouteName(route.getRouteName());
            respVO.setProductMaterialId(route.getProductMaterialId());
            respVO.setProductMaterialCode(route.getProductMaterialCode());
        }
        if (workOrder != null) {
            respVO.setLotNo(workOrder.getLotNo());
            respVO.setProductMaterialName(workOrder.getProductName());
        }
        respVO.setParamTemplateJson(routeOperation != null ? routeOperation.getParamTemplateJson() : null);
        respVO.setRecords(BeanUtils.toBean(hcParamRecordMapper.selectListByReportId(report.getId()), HcParamRecordRespVO.class));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitParamRecord(HcParamRecordSubmitReqVO reqVO) {
        HcSfcReportDO report = getRequiredReport(reqVO.getReportId(), reqVO.getReportNo());
        hcParamRecordMapper.deleteByReportId(report.getId());
        List<HcParamRecordDO> list = reqVO.getItems().stream().map(item -> {
            HcParamRecordDO entity = new HcParamRecordDO();
            entity.clean();
            entity.setId(null);
            entity.setReportId(report.getId());
            entity.setReportNo(report.getReportNo());
            entity.setParamCode(item.getParamCode());
            entity.setParamName(item.getParamName());
            entity.setParamValue(item.getParamValue());
            entity.setValueNum(item.getValueNum());
            entity.setUom(item.getUom());
            entity.setJudgeResult(item.getJudgeResult());
            return entity;
        }).toList();
        hcParamRecordMapper.insertBatch(list);
    }

    @Override
    public PageResult<HcParamRecordDO> getParamRecordPage(HcParamRecordPageReqVO pageReqVO) {
        return hcParamRecordMapper.selectPage(pageReqVO);
    }

    private HcSfcReportDO getRequiredReport(Long reportId, String reportNo) {
        HcSfcReportDO report = reportId != null ? hcSfcReportMapper.selectById(reportId) : null;
        if (report == null && reportNo != null && !reportNo.isBlank()) {
            report = hcSfcReportMapper.selectByReportNo(reportNo);
        }
        if (report == null) {
            throw exception(HCPARAMRECORD_REPORT_NOT_EXISTS);
        }
        return report;
    }
}
