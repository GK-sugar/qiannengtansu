package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("carbon_passport")
public class CarbonPassport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String passportNo;
    private Long applicationId;
    private Long orgId;
    private Long productId;
    private String productName;
    private String standard;
    private BigDecimal totalCo2e;
    private BigDecimal intensity;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String verifyToken;
    private Long qrImgFileId;
    private Long passportFileId;
    private String sealImgUrl;
    private String signerName;
    private String hashValue;
    private String status;
    private LocalDateTime issueTime;
    private LocalDateTime createTime;
}