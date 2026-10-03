package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HcVisualPrintImportRespVO {

    private String importBatchNo;
    private Integer totalCount = 0;
    private Integer createCount = 0;
    private Integer updateCount = 0;
    private Integer skipCount = 0;
    private List<String> messages = new ArrayList<>();

}
