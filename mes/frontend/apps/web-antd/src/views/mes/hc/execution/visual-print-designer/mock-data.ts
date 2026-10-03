// Auto-generated from soft pad checklist workbook. Keep UTF-8 without BOM.
import labelImage1 from '#/assets/mes/print-designer/ID_023A38800E014219B6208BB0C43A43BF.png';
import labelImage2 from '#/assets/mes/print-designer/ID_14B7EA2AB1E84124A26455330966566D.png';
import labelImage3 from '#/assets/mes/print-designer/ID_1E6B1C2D56C14021A2F02D6785DC1E2F.png';
import labelImage4 from '#/assets/mes/print-designer/ID_2BAB7DB17A80486C916778DD99A598BD.png';
import labelImage5 from '#/assets/mes/print-designer/ID_2E1381B10A4045DD9DD61F550B57E61D.png';
import labelImage6 from '#/assets/mes/print-designer/ID_2E14C02675C6483B9BE6B2AF3D750BEA.png';
import labelImage7 from '#/assets/mes/print-designer/ID_3CE22EE508524717BA2B17E17F545A73.png';
import labelImage8 from '#/assets/mes/print-designer/ID_525B824893374CB39A2F08874680F0DB.png';
import labelImage9 from '#/assets/mes/print-designer/ID_5F490F8E9523459384F1AC17599D3233.png';
import labelImage10 from '#/assets/mes/print-designer/ID_676D0BF104E045EE9B44C846D3A5E22D.png';
import labelImage11 from '#/assets/mes/print-designer/ID_698B369B3C8442A7A14BDD2705A0588B.png';
import labelImage12 from '#/assets/mes/print-designer/ID_79DEFDA095454ACBBA189AEE15D9DA27.png';
import labelImage13 from '#/assets/mes/print-designer/ID_816287B98425497E9D21CC7C921DAA5E.png';
import labelImage14 from '#/assets/mes/print-designer/ID_A1E06817044342BDB20EBCDC3274BCA0.png';
import labelImage15 from '#/assets/mes/print-designer/ID_A51C9112F87249EBAFA74317F3EE97D9.png';
import labelImage16 from '#/assets/mes/print-designer/ID_B2AD45ED492B4183A54282110EB77FA1.png';
import labelImage17 from '#/assets/mes/print-designer/ID_C32FED4FB99E491288CC753DD9F454AB.png';
import labelImage18 from '#/assets/mes/print-designer/ID_C3F39F4AF34E4A9A863D4F286CB9A6E7.png';
import labelImage19 from '#/assets/mes/print-designer/ID_CB2EAAC6CF624F948FF6CEE722AB8239.png';
import labelImage20 from '#/assets/mes/print-designer/ID_CDAA23130F4549D284DA1C72585B8AF4.png';
import labelImage21 from '#/assets/mes/print-designer/ID_D0DD1611F04E4F2486DB28E0256A802D.png';
import labelImage22 from '#/assets/mes/print-designer/ID_DBD42E81AE924CE5956719E83E1B079E.png';
import labelImage23 from '#/assets/mes/print-designer/ID_E0652AD177B3481289967632409B986A.png';
import labelImage24 from '#/assets/mes/print-designer/ID_E189F229E9E948BBB5F14B809CFA92AA.png';
import labelImage25 from '#/assets/mes/print-designer/ID_E69FFF290AE541B08799B8EB45E46CA0.png';
import labelImage26 from '#/assets/mes/print-designer/ID_ED9F8968265742779F996B4799361E74.png';
import labelImage27 from '#/assets/mes/print-designer/ID_EEB659BB262C48CC86ABB849EDA5B09E.png';
import labelImage28 from '#/assets/mes/print-designer/ID_F4CE8D322A204637927C7A462B0FCBFC.jpeg';
import labelImage29 from '#/assets/mes/print-designer/ID_F5326A90AB484E51B36073326E56509F.png';
import labelImage30 from '#/assets/mes/print-designer/ID_FAF5A727808C413FA270D862B8A51C2B.png';
import labelImage31 from '#/assets/mes/print-designer/ID_FCF909C83F2446C8BB1890ED6592FF33.png';

export type VisualLabelKind = 'padBack' | 'cleanBag' | 'boxFront' | 'customerSide';

export interface VisualPrintLabelVariant {
  kind: VisualLabelKind;
  label: string;
  imageId: string;
  imageFile: string;
  imageHeightPx: number;
  imageUrl: string;
  imageWidthPx: number;
  sourceCell: string;
  sourceRow: number;
  productType?: string;
  sizeMm?: string;
}

export interface VisualPrintProduct {
  productType: string;
  sizeMm: string;
  sourceRow: number;
}

export interface VisualPrintSeedRow {
  id: string;
  sourceRow: number;
  serialNo: string;
  customer: string;
  products: VisualPrintProduct[];
  labels: Record<VisualLabelKind, VisualPrintLabelVariant[]>;
  shipping: Record<string, string>;
  remarks: string[];
}

export const visualLabelKinds: Array<{ kind: VisualLabelKind; label: string; shortLabel: string }> = [
  { kind: 'padBack', label: 'pad背面标签', shortLabel: 'Pad背标' },
  { kind: 'cleanBag', label: '洁净袋标签', shortLabel: '洁净袋' },
  { kind: 'boxFront', label: '包装盒正面大标签', shortLabel: '盒正标' },
  { kind: 'customerSide', label: '客户侧标', shortLabel: '客户侧标' },
];

const imageMap: Record<string, string> = {
  "ID_023A38800E014219B6208BB0C43A43BF.png": labelImage1,
  "ID_14B7EA2AB1E84124A26455330966566D.png": labelImage2,
  "ID_1E6B1C2D56C14021A2F02D6785DC1E2F.png": labelImage3,
  "ID_2BAB7DB17A80486C916778DD99A598BD.png": labelImage4,
  "ID_2E1381B10A4045DD9DD61F550B57E61D.png": labelImage5,
  "ID_2E14C02675C6483B9BE6B2AF3D750BEA.png": labelImage6,
  "ID_3CE22EE508524717BA2B17E17F545A73.png": labelImage7,
  "ID_525B824893374CB39A2F08874680F0DB.png": labelImage8,
  "ID_5F490F8E9523459384F1AC17599D3233.png": labelImage9,
  "ID_676D0BF104E045EE9B44C846D3A5E22D.png": labelImage10,
  "ID_698B369B3C8442A7A14BDD2705A0588B.png": labelImage11,
  "ID_79DEFDA095454ACBBA189AEE15D9DA27.png": labelImage12,
  "ID_816287B98425497E9D21CC7C921DAA5E.png": labelImage13,
  "ID_A1E06817044342BDB20EBCDC3274BCA0.png": labelImage14,
  "ID_A51C9112F87249EBAFA74317F3EE97D9.png": labelImage15,
  "ID_B2AD45ED492B4183A54282110EB77FA1.png": labelImage16,
  "ID_C32FED4FB99E491288CC753DD9F454AB.png": labelImage17,
  "ID_C3F39F4AF34E4A9A863D4F286CB9A6E7.png": labelImage18,
  "ID_CB2EAAC6CF624F948FF6CEE722AB8239.png": labelImage19,
  "ID_CDAA23130F4549D284DA1C72585B8AF4.png": labelImage20,
  "ID_D0DD1611F04E4F2486DB28E0256A802D.png": labelImage21,
  "ID_DBD42E81AE924CE5956719E83E1B079E.png": labelImage22,
  "ID_E0652AD177B3481289967632409B986A.png": labelImage23,
  "ID_E189F229E9E948BBB5F14B809CFA92AA.png": labelImage24,
  "ID_E69FFF290AE541B08799B8EB45E46CA0.png": labelImage25,
  "ID_ED9F8968265742779F996B4799361E74.png": labelImage26,
  "ID_EEB659BB262C48CC86ABB849EDA5B09E.png": labelImage27,
  "ID_F4CE8D322A204637927C7A462B0FCBFC.jpeg": labelImage28,
  "ID_F5326A90AB484E51B36073326E56509F.png": labelImage29,
  "ID_FAF5A727808C413FA270D862B8A51C2B.png": labelImage30,
  "ID_FCF909C83F2446C8BB1890ED6592FF33.png": labelImage31,
};

const imageMetaMap: Record<string, { heightPx: number; widthPx: number }> = {
  "ID_023A38800E014219B6208BB0C43A43BF.png": { heightPx: 599, widthPx: 696 },
  "ID_14B7EA2AB1E84124A26455330966566D.png": { heightPx: 717, widthPx: 860 },
  "ID_1E6B1C2D56C14021A2F02D6785DC1E2F.png": { heightPx: 593, widthPx: 705 },
  "ID_2BAB7DB17A80486C916778DD99A598BD.png": { heightPx: 622, widthPx: 712 },
  "ID_2E1381B10A4045DD9DD61F550B57E61D.png": { heightPx: 573, widthPx: 813 },
  "ID_2E14C02675C6483B9BE6B2AF3D750BEA.png": { heightPx: 593, widthPx: 705 },
  "ID_3CE22EE508524717BA2B17E17F545A73.png": { heightPx: 721, widthPx: 865 },
  "ID_525B824893374CB39A2F08874680F0DB.png": { heightPx: 657, widthPx: 702 },
  "ID_5F490F8E9523459384F1AC17599D3233.png": { heightPx: 649, widthPx: 757 },
  "ID_676D0BF104E045EE9B44C846D3A5E22D.png": { heightPx: 851, widthPx: 983 },
  "ID_698B369B3C8442A7A14BDD2705A0588B.png": { heightPx: 718, widthPx: 859 },
  "ID_79DEFDA095454ACBBA189AEE15D9DA27.png": { heightPx: 665, widthPx: 731 },
  "ID_816287B98425497E9D21CC7C921DAA5E.png": { heightPx: 719, widthPx: 864 },
  "ID_A1E06817044342BDB20EBCDC3274BCA0.png": { heightPx: 542, widthPx: 821 },
  "ID_A51C9112F87249EBAFA74317F3EE97D9.png": { heightPx: 751, widthPx: 739 },
  "ID_B2AD45ED492B4183A54282110EB77FA1.png": { heightPx: 586, widthPx: 706 },
  "ID_C32FED4FB99E491288CC753DD9F454AB.png": { heightPx: 604, widthPx: 1206 },
  "ID_C3F39F4AF34E4A9A863D4F286CB9A6E7.png": { heightPx: 715, widthPx: 795 },
  "ID_CB2EAAC6CF624F948FF6CEE722AB8239.png": { heightPx: 475, widthPx: 1700 },
  "ID_CDAA23130F4549D284DA1C72585B8AF4.png": { heightPx: 586, widthPx: 706 },
  "ID_D0DD1611F04E4F2486DB28E0256A802D.png": { heightPx: 581, widthPx: 704 },
  "ID_DBD42E81AE924CE5956719E83E1B079E.png": { heightPx: 379, widthPx: 763 },
  "ID_E0652AD177B3481289967632409B986A.png": { heightPx: 713, widthPx: 865 },
  "ID_E189F229E9E948BBB5F14B809CFA92AA.png": { heightPx: 715, widthPx: 861 },
  "ID_E69FFF290AE541B08799B8EB45E46CA0.png": { heightPx: 451, widthPx: 877 },
  "ID_ED9F8968265742779F996B4799361E74.png": { heightPx: 541, widthPx: 967 },
  "ID_EEB659BB262C48CC86ABB849EDA5B09E.png": { heightPx: 593, widthPx: 705 },
  "ID_F4CE8D322A204637927C7A462B0FCBFC.jpeg": { heightPx: 1316, widthPx: 1472 },
  "ID_F5326A90AB484E51B36073326E56509F.png": { heightPx: 563, widthPx: 678 },
  "ID_FAF5A727808C413FA270D862B8A51C2B.png": { heightPx: 718, widthPx: 863 },
  "ID_FCF909C83F2446C8BB1890ED6592FF33.png": { heightPx: 581, widthPx: 704 },
};

export const visualPrintImageMap = imageMap;

export const visualPrintImageMetaMap = imageMetaMap;

export function resolveVisualPrintImageUrl(imageFile?: string) {
  return imageFile ? imageMap[imageFile] || '' : '';
}

const rawSeedRows = [
  {
    "id": "excel-row-2",
    "sourceRow": 2,
    "serialNo": "1",
    "customer": "HC.618-K0008",
    "products": [
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 2
      },
      {
        "productType": "DTP0201",
        "sizeMm": "740",
        "sourceRow": 3
      },
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 4
      },
      {
        "productType": "DTP0203",
        "sizeMm": "740",
        "sourceRow": 5
      },
      {
        "productType": "DTP0100",
        "sizeMm": "775",
        "sourceRow": 6
      }
    ],
    "labels": {
      "padBack": [],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_B2AD45ED492B4183A54282110EB77FA1",
          "imageFile": "ID_B2AD45ED492B4183A54282110EB77FA1.png",
          "sourceCell": "F2",
          "sourceRow": 2,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_EEB659BB262C48CC86ABB849EDA5B09E",
          "imageFile": "ID_EEB659BB262C48CC86ABB849EDA5B09E.png",
          "sourceCell": "G2",
          "sourceRow": 2,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_79DEFDA095454ACBBA189AEE15D9DA27",
          "imageFile": "ID_79DEFDA095454ACBBA189AEE15D9DA27.png",
          "sourceCell": "H2",
          "sourceRow": 2,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*90mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "禾臣快递到基石，基石转客户",
      "是否需要随货纸版COA": "否",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "基石仓库送货时给客户仓库",
      "随货文件包装方式": "分不同料号打托盘，唛头在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "打托盘时单盒不包缠绕膜，整体打包好后多盒一起缠绕4层缠绕膜，四面包护角"
    },
    "remarks": [
      "打托盘时单盒不包缠绕膜，整体打包好后多盒一起缠绕4层缠绕膜，四面包护角"
    ]
  },
  {
    "id": "excel-row-7",
    "sourceRow": 7,
    "serialNo": "2",
    "customer": "HC.618-K0010",
    "products": [
      {
        "productType": "DTP0213",
        "sizeMm": "775",
        "sourceRow": 7
      },
      {
        "productType": "DTP0213",
        "sizeMm": "740",
        "sourceRow": 8
      },
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 9
      },
      {
        "productType": "DTP0201",
        "sizeMm": "740",
        "sourceRow": 10
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_023A38800E014219B6208BB0C43A43BF",
          "imageFile": "ID_023A38800E014219B6208BB0C43A43BF.png",
          "sourceCell": "E7",
          "sourceRow": 7,
          "productType": "DTP0213",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_023A38800E014219B6208BB0C43A43BF",
          "imageFile": "ID_023A38800E014219B6208BB0C43A43BF.png",
          "sourceCell": "F7",
          "sourceRow": 7,
          "productType": "DTP0213",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_5F490F8E9523459384F1AC17599D3233",
          "imageFile": "ID_5F490F8E9523459384F1AC17599D3233.png",
          "sourceCell": "G7",
          "sourceRow": 7,
          "productType": "DTP0213",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_E0652AD177B3481289967632409B986A",
          "imageFile": "ID_E0652AD177B3481289967632409B986A.png",
          "sourceCell": "H7",
          "sourceRow": 7,
          "productType": "DTP0213",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*90mm",
      "客户侧标方式": "基石销售录入批次后客户系统导出",
      "发货方式": "基石物流车从禾臣提货直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "纸质送货单一式三份给到物流车司机",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "该客户740mm尺寸产品信息需表示为“740mm\",775mm与其他产品保持一致为“30.5\"”"
    },
    "remarks": [
      "该客户740mm尺寸产品信息需表示为“740mm\",775mm与其他产品保持一致为“30.5\"”"
    ]
  },
  {
    "id": "excel-row-11",
    "sourceRow": 11,
    "serialNo": "3",
    "customer": "HC.618-K0016",
    "products": [
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 11
      },
      {
        "productType": "DTP0201",
        "sizeMm": "740",
        "sourceRow": 12
      },
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 13
      },
      {
        "productType": "DTP0203-E",
        "sizeMm": "740",
        "sourceRow": 14
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_CDAA23130F4549D284DA1C72585B8AF4",
          "imageFile": "ID_CDAA23130F4549D284DA1C72585B8AF4.png",
          "sourceCell": "E11",
          "sourceRow": 11,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_B2AD45ED492B4183A54282110EB77FA1",
          "imageFile": "ID_B2AD45ED492B4183A54282110EB77FA1.png",
          "sourceCell": "F11",
          "sourceRow": 11,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_EEB659BB262C48CC86ABB849EDA5B09E",
          "imageFile": "ID_EEB659BB262C48CC86ABB849EDA5B09E.png",
          "sourceCell": "G11",
          "sourceRow": 11,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_C3F39F4AF34E4A9A863D4F286CB9A6E7",
          "imageFile": "ID_C3F39F4AF34E4A9A863D4F286CB9A6E7.png",
          "sourceCell": "H11",
          "sourceRow": 11,
          "productType": "DTP0201",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*90mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "基石物流车从禾臣提货直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "纸质送货单一式三份给到物流车司机",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "该客户需要在包装盒上用空白标签打印“样品”,贴在侧标旁边，否则客户拒收"
    },
    "remarks": [
      "该客户需要在包装盒上用空白标签打印“样品”,贴在侧标旁边，否则客户拒收"
    ]
  },
  {
    "id": "excel-row-15",
    "sourceRow": 15,
    "serialNo": "4",
    "customer": "HC.618-K0009",
    "products": [
      {
        "productType": "DTP0201",
        "sizeMm": "740",
        "sourceRow": 15
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_CDAA23130F4549D284DA1C72585B8AF4",
          "imageFile": "ID_CDAA23130F4549D284DA1C72585B8AF4.png",
          "sourceCell": "E15",
          "sourceRow": 15,
          "productType": "DTP0201",
          "sizeMm": "740"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_B2AD45ED492B4183A54282110EB77FA1",
          "imageFile": "ID_B2AD45ED492B4183A54282110EB77FA1.png",
          "sourceCell": "F15",
          "sourceRow": 15,
          "productType": "DTP0201",
          "sizeMm": "740"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_EEB659BB262C48CC86ABB849EDA5B09E",
          "imageFile": "ID_EEB659BB262C48CC86ABB849EDA5B09E.png",
          "sourceCell": "G15",
          "sourceRow": 15,
          "productType": "DTP0201",
          "sizeMm": "740"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_A51C9112F87249EBAFA74317F3EE97D9",
          "imageFile": "ID_A51C9112F87249EBAFA74317F3EE97D9.png",
          "sourceCell": "H15",
          "sourceRow": 15,
          "productType": "DTP0201",
          "sizeMm": "740"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*90mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "禾臣快递到基石，基石转客户",
      "是否需要随货纸版COA": "否",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "基石仓库送货时给客户仓库",
      "随货文件包装方式": "分不同料号打托盘，唛头在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "每一盒单独打包，有缠绕膜有打包带"
    },
    "remarks": [
      "每一盒单独打包，有缠绕膜有打包带"
    ]
  },
  {
    "id": "excel-row-16",
    "sourceRow": 16,
    "serialNo": "5",
    "customer": "HC.618-K0019",
    "products": [
      {
        "productType": "DTP0201-L",
        "sizeMm": "775",
        "sourceRow": 16
      },
      {
        "productType": "DTP0201-E",
        "sizeMm": "740",
        "sourceRow": 17
      },
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 18
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_14B7EA2AB1E84124A26455330966566D",
          "imageFile": "ID_14B7EA2AB1E84124A26455330966566D.png",
          "sourceCell": "E16",
          "sourceRow": 16,
          "productType": "DTP0201-L",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_14B7EA2AB1E84124A26455330966566D",
          "imageFile": "ID_14B7EA2AB1E84124A26455330966566D.png",
          "sourceCell": "F16",
          "sourceRow": 16,
          "productType": "DTP0201-L",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_698B369B3C8442A7A14BDD2705A0588B",
          "imageFile": "ID_698B369B3C8442A7A14BDD2705A0588B.png",
          "sourceCell": "G16",
          "sourceRow": 16,
          "productType": "DTP0201-L",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_CB2EAAC6CF624F948FF6CEE722AB8239",
          "imageFile": "ID_CB2EAAC6CF624F948FF6CEE722AB8239.png",
          "sourceCell": "H16",
          "sourceRow": 16,
          "productType": "DTP0201-L",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*50mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "禾臣快递直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "随货包装",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头和送货单在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "无特殊要求"
    },
    "remarks": [
      "无特殊要求"
    ]
  },
  {
    "id": "excel-row-19",
    "sourceRow": 19,
    "serialNo": "6",
    "customer": "HC.618-K0003/K0042/K0044",
    "products": [
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 19
      },
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 22
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_FCF909C83F2446C8BB1890ED6592FF33",
          "imageFile": "ID_FCF909C83F2446C8BB1890ED6592FF33.png",
          "sourceCell": "E19",
          "sourceRow": 19,
          "productType": "DTP0203",
          "sizeMm": "775"
        },
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_676D0BF104E045EE9B44C846D3A5E22D",
          "imageFile": "ID_676D0BF104E045EE9B44C846D3A5E22D.png",
          "sourceCell": "E20",
          "sourceRow": 20,
          "productType": "",
          "sizeMm": ""
        },
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_ED9F8968265742779F996B4799361E74",
          "imageFile": "ID_ED9F8968265742779F996B4799361E74.png",
          "sourceCell": "E21",
          "sourceRow": 21,
          "productType": "",
          "sizeMm": ""
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_FCF909C83F2446C8BB1890ED6592FF33",
          "imageFile": "ID_FCF909C83F2446C8BB1890ED6592FF33.png",
          "sourceCell": "F19",
          "sourceRow": 19,
          "productType": "DTP0203",
          "sizeMm": "775"
        },
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_676D0BF104E045EE9B44C846D3A5E22D",
          "imageFile": "ID_676D0BF104E045EE9B44C846D3A5E22D.png",
          "sourceCell": "F20",
          "sourceRow": 20,
          "productType": "",
          "sizeMm": ""
        },
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_ED9F8968265742779F996B4799361E74",
          "imageFile": "ID_ED9F8968265742779F996B4799361E74.png",
          "sourceCell": "F21",
          "sourceRow": 21,
          "productType": "",
          "sizeMm": ""
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_2E14C02675C6483B9BE6B2AF3D750BEA",
          "imageFile": "ID_2E14C02675C6483B9BE6B2AF3D750BEA.png",
          "sourceCell": "G19",
          "sourceRow": 19,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_E69FFF290AE541B08799B8EB45E46CA0",
          "imageFile": "ID_E69FFF290AE541B08799B8EB45E46CA0.png",
          "sourceCell": "H19",
          "sourceRow": 19,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*50mm",
      "客户侧标方式": "单片包装需要3个标签，客户条形码标签（100*90mm)自行编辑，客户最小标签（100*50mm)和客户侧标需销售录入批次后客户系统导出",
      "发货方式": "禾臣快递直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "否",
      "是否有唛头": "是",
      "送货单": "随货包装",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头和送货单在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "无特殊要求"
    },
    "remarks": [
      "无特殊要求"
    ]
  },
  {
    "id": "excel-row-23",
    "sourceRow": 23,
    "serialNo": "7",
    "customer": "客户HC.618-K0061",
    "products": [
      {
        "productType": "SLT0200",
        "sizeMm": "830",
        "sourceRow": 23
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_816287B98425497E9D21CC7C921DAA5E",
          "imageFile": "ID_816287B98425497E9D21CC7C921DAA5E.png",
          "sourceCell": "E23",
          "sourceRow": 23,
          "productType": "SLT0200",
          "sizeMm": "830"
        },
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_DBD42E81AE924CE5956719E83E1B079E",
          "imageFile": "ID_DBD42E81AE924CE5956719E83E1B079E.png",
          "sourceCell": "E24",
          "sourceRow": 24,
          "productType": "",
          "sizeMm": ""
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_816287B98425497E9D21CC7C921DAA5E",
          "imageFile": "ID_816287B98425497E9D21CC7C921DAA5E.png",
          "sourceCell": "F23",
          "sourceRow": 23,
          "productType": "SLT0200",
          "sizeMm": "830"
        },
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_DBD42E81AE924CE5956719E83E1B079E",
          "imageFile": "ID_DBD42E81AE924CE5956719E83E1B079E.png",
          "sourceCell": "F24",
          "sourceRow": 24,
          "productType": "",
          "sizeMm": ""
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_E189F229E9E948BBB5F14B809CFA92AA",
          "imageFile": "ID_E189F229E9E948BBB5F14B809CFA92AA.png",
          "sourceCell": "G23",
          "sourceRow": 23,
          "productType": "SLT0200",
          "sizeMm": "830"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_C32FED4FB99E491288CC753DD9F454AB",
          "imageFile": "ID_C32FED4FB99E491288CC753DD9F454AB.png",
          "sourceCell": "H23",
          "sourceRow": 23,
          "productType": "SLT0200",
          "sizeMm": "830"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*50mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "禾臣快递直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "否",
      "是否有唛头": "是",
      "送货单": "随货包装",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头和送货单在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "830mm需拆掉纸盒泡沫包装"
    },
    "remarks": [
      "830mm需拆掉纸盒泡沫包装"
    ]
  },
  {
    "id": "excel-row-25",
    "sourceRow": 25,
    "serialNo": "8",
    "customer": "客户HC.618-K0013",
    "products": [
      {
        "productType": "DT-P0102",
        "sizeMm": "775",
        "sourceRow": 25
      },
      {
        "productType": "DT-P0102",
        "sizeMm": "740",
        "sourceRow": 26
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_FAF5A727808C413FA270D862B8A51C2B",
          "imageFile": "ID_FAF5A727808C413FA270D862B8A51C2B.png",
          "sourceCell": "E25",
          "sourceRow": 25,
          "productType": "DT-P0102",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_FAF5A727808C413FA270D862B8A51C2B",
          "imageFile": "ID_FAF5A727808C413FA270D862B8A51C2B.png",
          "sourceCell": "F25",
          "sourceRow": 25,
          "productType": "DT-P0102",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_3CE22EE508524717BA2B17E17F545A73",
          "imageFile": "ID_3CE22EE508524717BA2B17E17F545A73.png",
          "sourceCell": "G25",
          "sourceRow": 25,
          "productType": "DT-P0102",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_F4CE8D322A204637927C7A462B0FCBFC",
          "imageFile": "ID_F4CE8D322A204637927C7A462B0FCBFC.jpeg",
          "sourceCell": "H25",
          "sourceRow": 25,
          "productType": "DT-P0102",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*120mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "禾臣快递直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "否",
      "是否有唛头": "是",
      "送货单": "随货包装",
      "随货文件包装方式": "分不同料号打托盘，纸质COA、唛头和送货单在托盘正上方用自封袋包装，粘贴随货文件，只装一份",
      "特殊备注": "纸质COA盖章，彩印"
    },
    "remarks": [
      "纸质COA盖章，彩印"
    ]
  },
  {
    "id": "excel-row-27",
    "sourceRow": 27,
    "serialNo": "9",
    "customer": "客户HC.618-K0006",
    "products": [
      {
        "productType": "DTP0200",
        "sizeMm": "775",
        "sourceRow": 27
      },
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 28
      },
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 29
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_CDAA23130F4549D284DA1C72585B8AF4",
          "imageFile": "ID_CDAA23130F4549D284DA1C72585B8AF4.png",
          "sourceCell": "E27",
          "sourceRow": 27,
          "productType": "DTP0200",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_B2AD45ED492B4183A54282110EB77FA1",
          "imageFile": "ID_B2AD45ED492B4183A54282110EB77FA1.png",
          "sourceCell": "F27",
          "sourceRow": 27,
          "productType": "DTP0200",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_EEB659BB262C48CC86ABB849EDA5B09E",
          "imageFile": "ID_EEB659BB262C48CC86ABB849EDA5B09E.png",
          "sourceCell": "G27",
          "sourceRow": 27,
          "productType": "DTP0200",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_A1E06817044342BDB20EBCDC3274BCA0",
          "imageFile": "ID_A1E06817044342BDB20EBCDC3274BCA0.png",
          "sourceCell": "H27",
          "sourceRow": 27,
          "productType": "DTP0200",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*90mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "基石物流车从禾臣提货直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "纸质送货单一式三份给到物流车司机",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "该客户需要在包装盒上用空白标签打印“样品”,贴在侧标旁边，且送货单备注一栏应注明“样品”，否则客户拒收。"
    },
    "remarks": [
      "该客户需要在包装盒上用空白标签打印“样品”,贴在侧标旁边，且送货单备注一栏应注明“样品”，否则客户拒收。"
    ]
  },
  {
    "id": "excel-row-30",
    "sourceRow": 30,
    "serialNo": "10",
    "customer": "客户HC.618-K0031",
    "products": [
      {
        "productType": "DTP0100",
        "sizeMm": "775",
        "sourceRow": 30
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_F5326A90AB484E51B36073326E56509F",
          "imageFile": "ID_F5326A90AB484E51B36073326E56509F.png",
          "sourceCell": "E30",
          "sourceRow": 30,
          "productType": "DTP0100",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_F5326A90AB484E51B36073326E56509F",
          "imageFile": "ID_F5326A90AB484E51B36073326E56509F.png",
          "sourceCell": "F30",
          "sourceRow": 30,
          "productType": "DTP0100",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_F5326A90AB484E51B36073326E56509F",
          "imageFile": "ID_F5326A90AB484E51B36073326E56509F.png",
          "sourceCell": "G30",
          "sourceRow": 30,
          "productType": "DTP0100",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_525B824893374CB39A2F08874680F0DB",
          "imageFile": "ID_525B824893374CB39A2F08874680F0DB.png",
          "sourceCell": "H30",
          "sourceRow": 30,
          "productType": "DTP0100",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "85*60mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "禾臣快递直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "随货包装",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头和送货单在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "跨越派送前需要提前申请入场，车辆信息"
    },
    "remarks": [
      "跨越派送前需要提前申请入场，车辆信息"
    ]
  },
  {
    "id": "excel-row-31",
    "sourceRow": 31,
    "serialNo": "11",
    "customer": "客户HC.618-K0004",
    "products": [
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 31
      },
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 32
      },
      {
        "productType": "DTP0201",
        "sizeMm": "740",
        "sourceRow": 33
      },
      {
        "productType": "DTP0100",
        "sizeMm": "775",
        "sourceRow": 34
      },
      {
        "productType": "DTP0100",
        "sizeMm": "740",
        "sourceRow": 35
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_CDAA23130F4549D284DA1C72585B8AF4",
          "imageFile": "ID_CDAA23130F4549D284DA1C72585B8AF4.png",
          "sourceCell": "E31",
          "sourceRow": 31,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_B2AD45ED492B4183A54282110EB77FA1",
          "imageFile": "ID_B2AD45ED492B4183A54282110EB77FA1.png",
          "sourceCell": "F31",
          "sourceRow": 31,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_EEB659BB262C48CC86ABB849EDA5B09E",
          "imageFile": "ID_EEB659BB262C48CC86ABB849EDA5B09E.png",
          "sourceCell": "G31",
          "sourceRow": 31,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_1E6B1C2D56C14021A2F02D6785DC1E2F",
          "imageFile": "ID_1E6B1C2D56C14021A2F02D6785DC1E2F.png",
          "sourceCell": "H31",
          "sourceRow": 31,
          "productType": "DTP0203",
          "sizeMm": "775"
        },
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_2BAB7DB17A80486C916778DD99A598BD",
          "imageFile": "ID_2BAB7DB17A80486C916778DD99A598BD.png",
          "sourceCell": "H34",
          "sourceRow": 34,
          "productType": "DTP0100",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*120mm",
      "客户侧标方式": "自行更改编辑",
      "发货方式": "基石物流车从禾臣提货直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "纸质送货单一式三份给到物流车司机",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "无特殊要求"
    },
    "remarks": [
      "无特殊要求"
    ]
  },
  {
    "id": "excel-row-36",
    "sourceRow": 36,
    "serialNo": "12",
    "customer": "客户HC.618-K0028",
    "products": [
      {
        "productType": "DTP0203",
        "sizeMm": "775",
        "sourceRow": 36
      },
      {
        "productType": "DTP0203",
        "sizeMm": "740",
        "sourceRow": 37
      },
      {
        "productType": "DTP0201",
        "sizeMm": "775",
        "sourceRow": 38
      }
    ],
    "labels": {
      "padBack": [
        {
          "kind": "padBack",
          "label": "pad背面标签",
          "imageId": "ID_D0DD1611F04E4F2486DB28E0256A802D",
          "imageFile": "ID_D0DD1611F04E4F2486DB28E0256A802D.png",
          "sourceCell": "E36",
          "sourceRow": 36,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "cleanBag": [
        {
          "kind": "cleanBag",
          "label": "洁净袋标签",
          "imageId": "ID_D0DD1611F04E4F2486DB28E0256A802D",
          "imageFile": "ID_D0DD1611F04E4F2486DB28E0256A802D.png",
          "sourceCell": "F36",
          "sourceRow": 36,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "boxFront": [
        {
          "kind": "boxFront",
          "label": "包装盒正面大标签",
          "imageId": "ID_D0DD1611F04E4F2486DB28E0256A802D",
          "imageFile": "ID_D0DD1611F04E4F2486DB28E0256A802D.png",
          "sourceCell": "G36",
          "sourceRow": 36,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ],
      "customerSide": [
        {
          "kind": "customerSide",
          "label": "客户侧标",
          "imageId": "ID_2E1381B10A4045DD9DD61F550B57E61D",
          "imageFile": "ID_2E1381B10A4045DD9DD61F550B57E61D.png",
          "sourceCell": "H36",
          "sourceRow": 36,
          "productType": "DTP0203",
          "sizeMm": "775"
        }
      ]
    },
    "shipping": {
      "客户侧标尺寸": "100*90mm",
      "客户侧标方式": "基石销售录入批次后客户系统导出",
      "发货方式": "禾臣快递直发客户",
      "是否需要随货纸版COA": "是",
      "是否需要ECOA": "是",
      "是否有唛头": "是",
      "送货单": "随货包装",
      "随货文件包装方式": "纸质COA每盒放一份，分不同料号打托盘，唛头和送货单在托盘正上方用自封袋包装，粘贴随货文件",
      "特殊备注": "正常用100*90mm的标签打印，如放不下则打印普通标签后用A4纸打印客户侧标"
    },
    "remarks": [
      "正常用100*90mm的标签打印，如放不下则打印普通标签后用A4纸打印客户侧标"
    ]
  }
] as Array<Omit<VisualPrintSeedRow, 'labels'> & {
  labels: Record<VisualLabelKind, Array<Omit<VisualPrintLabelVariant, 'imageHeightPx' | 'imageUrl' | 'imageWidthPx'>>>;
}>;

export const visualPrintSeedRows: VisualPrintSeedRow[] = rawSeedRows.map((row) => ({
  ...row,
  labels: Object.fromEntries(
    Object.entries(row.labels).map(([kind, variants]) => [
      kind,
      variants.map((variant) => ({
        ...variant,
        imageHeightPx: imageMetaMap[variant.imageFile]?.heightPx || 0,
        imageUrl: imageMap[variant.imageFile] || '',
        imageWidthPx: imageMetaMap[variant.imageFile]?.widthPx || 0,
      })),
    ]),
  ) as Record<VisualLabelKind, VisualPrintLabelVariant[]>,
}));

export const visualPrintSampleData = {
  productModel: 'DTP0201',
  productTypeName: '化学机械研磨垫',
  productNo: '10190065',
  productInfo: 'DTP0201, T09X30D7W10-E1, 30.5"',
  quantity: '1片',
  padNo: '075',
  batchNo: '62260418003',
  productionDate: '2026-04-18',
  expirationDate: '2027-02-17',
  plant: '7001',
  pn: 'SM430002TEST',
  materialDescription: 'DTP0203,30.5"',
  vendorPn: '',
  info: 'SM430002TEST202601260000887S',
  packageIndex: '1/5',
  expDate: '2026-10-29',
  shippingDate: '2026-01-27',
  purUom: 'PC',
  po: '6400005737',
};
