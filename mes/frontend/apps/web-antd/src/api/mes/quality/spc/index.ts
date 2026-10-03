import { requestClient } from '#/api/request';

export namespace MesSpcApi {
  export interface SubgroupData {
    groupId: string;
    time: string;
    values: number[];
    xBar: number;
    rValue: number;
    isOoc: boolean; // 是否失控 Out of Control
    oocRule?: string;
  }

  export interface SpcConfig {
    target: number;
    usl: number;
    lsl: number;
    uclX: number;
    lclX: number;
    clX: number;
    uclR: number;
    lclR: number;
    clR: number;
    cpk: number;
    cp: number;
  }

  export interface SpcAnalysisResult {
    config: SpcConfig;
    data: SubgroupData[];
  }
}

// 模拟生成涂布厚度 SPC 数据 (目标值 50，公差 ±5)
export async function getSpcData(machineCode: string, materialCode: string): Promise<MesSpcApi.SpcAnalysisResult> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const target = 50.0;
      const usl = 55.0;
      const lsl = 45.0;
      const subgroups: MesSpcApi.SubgroupData[] = [];
      const n = 5; // 子组大小

      let sumXBar = 0;
      let sumR = 0;

      // 生成 30 个子组的历史数据
      for (let i = 1; i <= 30; i++) {
        const values: number[] = [];
        let max = -Infinity;
        let min = Infinity;
        let sum = 0;

        // 模拟正常波动 (正态分布近似)
        for (let j = 0; j < n; j++) {
          // 制造第 28 组的异常突变 (模拟工艺失控)
          const baseOffset = i === 28 ? 3.5 : 0;
          const val = Number((target + baseOffset + (Math.random() * 4 - 2)).toFixed(2));
          values.push(val);
          sum += val;
          if (val > max) max = val;
          if (val < min) min = val;
        }

        const xBar = Number((sum / n).toFixed(2));
        const rValue = Number((max - min).toFixed(2));

        sumXBar += xBar;
        sumR += rValue;

        // 简化的失控规则校验标记 (实际应基于后端复杂算法)
        const isOoc = xBar > 52.5 || xBar < 47.5;

        subgroups.push({
          groupId: `G-${String(i).padStart(3, '0')}`,
          time: `02-22 ${String(Math.floor(i / 2) + 8).padStart(2, '0')}:${i % 2 === 0 ? '30' : '00'}`,
          values, xBar, rValue, isOoc,
          oocRule: isOoc ? '准则1: 单点超出控制界限(3σ)' : undefined
        });
      }

      // 常数系数 A2, D4, D3 for n=5
      const A2 = 0.577; const D4 = 2.114; const D3 = 0;
      const clX = Number((sumXBar / 30).toFixed(2));
      const clR = Number((sumR / 30).toFixed(2));

      const uclX = Number((clX + A2 * clR).toFixed(2));
      const lclX = Number((clX - A2 * clR).toFixed(2));
      const uclR = Number((D4 * clR).toFixed(2));
      const lclR = Number((D3 * clR).toFixed(2));

      // 简化估算标准差 sigma = R_bar / d2 (d2 approx 2.326 for n=5)
      const sigma = clR / 2.326;
      const cp = Number(((usl - lsl) / (6 * sigma)).toFixed(2));
      const cpk = Number(Math.min((usl - clX) / (3 * sigma), (clX - lsl) / (3 * sigma)).toFixed(2));

      resolve({
        config: { target, usl, lsl, uclX, lclX, clX, uclR, lclR, clR, cpk, cp },
        data: subgroups
      });
    }, 400);
  });
}
