package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordRespVO;
import java.util.List;

public interface HcAdhesiveProductionRecordService {

    PageResult<HcAdhesiveProductionRecordRespVO> getAdhesive1Page(
            HcAdhesiveProductionRecordPageReqVO reqVO);

    List<HcAdhesiveProductionRecordRespVO> getAdhesive1List(
            HcAdhesiveProductionRecordPageReqVO reqVO);

    PageResult<HcAdhesiveProductionRecordRespVO> getAdhesive2Page(
            HcAdhesiveProductionRecordPageReqVO reqVO);

    List<HcAdhesiveProductionRecordRespVO> getAdhesive2List(
            HcAdhesiveProductionRecordPageReqVO reqVO);

}
