package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商名称防重检查 Response VO")
@Data
public class SrmSupplierNameCheckRespVO {

    private String normalizedName;
    private List<SrmSupplierCandidateRespVO> registeredMatches;
    private List<SrmSupplierCandidateRespVO> pendingMatches;

}
