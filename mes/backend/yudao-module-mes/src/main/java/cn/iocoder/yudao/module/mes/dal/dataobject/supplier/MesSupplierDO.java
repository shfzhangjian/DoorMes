package cn.iocoder.yudao.module.mes.dal.dataobject.supplier;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商主数据 DO
 *
 * @author 演示管理员
 */
@TableName("mes_supplier")
@KeySequence("mes_supplier_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesSupplierDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 供应商编码
     */
    private String supplierCode;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 使用部门
     */
    private String usingDepartment;
    /**
     * 简称
     */
    private String shortName;
    /**
     * 联系人
     */
    private String contactPerson;
    /**
     * 联系电话
     */
    private String contactPhone;
    /**
     * 电子邮箱
     */
    private String email;
    /**
     * 详细地址
     */
    private String address;
    /**
     * 企业性质
     */
    private String companyNature;
    /**
     * 法定代表人
     */
    private String legalPerson;
    /**
     * 注册资本(万元)
     */
    private BigDecimal registeredCapital;
    /**
     * 成立日期
     */
    private LocalDate establishDate;
    /**
     * 产地
     */
    private String originPlace;
    /**
     * 原厂信息
     */
    private String originalFactoryInfo;
    /**
     * 主营产品
     */
    private String mainProducts;
    /**
     * 提供/协作产品
     */
    private String providedProduct;
    /**
     * 型号
     */
    private String model;
    /**
     * 物料代码
     */
    private String materialCode;
    /**
     * 适用产品
     */
    private String applicableProduct;
    /**
     * 导入日期
     */
    private LocalDate importDate;
    /**
     * 历史物料类别，保留兼容
     */
    private String materialCategory;
    /**
     * 物料等级: A/B/C/D
     */
    private String materialGrade;
    /**
     * 结算付款条件
     */
    private String paymentTerms;
    /**
     * 交货方式
     */
    private String deliveryMethod;
    /**
     * 供应商资源状态: PENDING(考察中), QUALIFIED(合格), UNQUALIFIED(不合格), FROZEN(冻结), ELIMINATED(淘汰), EXITED(退出)
     */
    private String status;
    /**
     * 来源基本情况调查表ID
     */
    private Long sourceSurveyId;
    /**
     * 来源基本情况调查表编号
     */
    private String sourceSurveyNo;
    /**
     * 登记人ID
     */
    private Long registrarId;
    /**
     * 登记人
     */
    private String registrarName;
    /**
     * 初始化说明
     */
    private String initializationReason;
    /**
     * 供应商名录管理范围ID
     */
    private Long scopeId;
    /**
     * 供应商名录管理范围编号
     */
    private String scopeCode;
    /**
     * 供应商名录管理范围名称
     */
    private String scopeName;
    /**
     * 等级: A/B/C/D
     */
    private String level;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 乐观锁
     */
    @Version
    private Integer version;

    private Long tenantId;


}
