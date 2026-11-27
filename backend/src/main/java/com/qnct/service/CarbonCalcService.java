package com.qnct.service;

import com.qnct.dto.CalcRequest;
import com.qnct.dto.CalcResultDto;

/**
 * 碳排放核算引擎（F1~F5）：
 *   F1 范围一：E = AD × NCV × EF × OF（化石燃料燃烧）
 *   F2 范围二：E = AD(外购电力) × EF_电网
 *   F3 范围三：E = AD(周转量/里程) × EF_运输
 *   F4 总量：E_total = ΣF1 + ΣF2 + ΣF3
 *   F5 碳强度：I = E_total / 产品产量
 * 入参因子已按 kgCO2e/unit 快照给出，引擎直接乘积汇聚。
 */
public interface CarbonCalcService {

    CalcResultDto calc(CalcRequest req);
}