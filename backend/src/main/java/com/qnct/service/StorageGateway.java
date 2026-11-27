package com.qnct.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * 文件存储网关：默认本地磁盘（./data），配置 STORAGE_TYPE=oss 时走 OSS。
 * OSS【此处需填入API Key】申请：https://ram.console.aliyun.com/ AccessKey + OSS Bucket
 */
@Slf4j
@Service
public class StorageGateway {

    @Value("${app.storage.type:local}")
    private String type;

    @Value("${app.storage.local-dir:./data}")
    private String localDir;

    @Value("${app.storage.oss.endpoint:}")
    private String endpoint;

    @Value("${app.storage.oss.bucket:}")
    private String bucket;

    @Value("${app.storage.oss.access-key-id:}")
    private String ak;

    @Value("${app.storage.oss.access-key-secret:}")
    private String sk;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(Paths.get(localDir));
        } catch (IOException ignored) {
        }
    }

    /** 保存文件，返回访问相对路径（本地模式直接返回 /files/... 供前端预览） */
    public String save(String dir, String ext, byte[] content) {
        if ("oss".equalsIgnoreCase(type)) {
            String key = dir + "/" + UUID.randomUUID() + (ext.startsWith(".") ? ext : "." + ext);
            com.aliyun.oss.OSS client = null;
            try {
                client = new com.aliyun.oss.OSSClientBuilder().build(endpoint, ak, sk);
                client.putObject(bucket, key, new java.io.ByteArrayInputStream(content));
                return client.generatePresignedUrl(bucket, key, new java.util.Date(System.currentTimeMillis() + 86400000L)).toString();
            } catch (Exception e) {
                log.warn("OSS 上传失败，降级本地：{}", e.getMessage());
            } finally {
                if (client != null) {
                    client.shutdown();
                }
            }
        }
        String file = localDir + "/" + dir + "/" + UUID.randomUUID() + (ext.startsWith(".") ? ext : "." + ext);
        try {
            Path p = Paths.get(file);
            Files.createDirectories(p.getParent());
            Files.write(p, content);
            return "/files/" + dir + "/" + p.getFileName();
        } catch (IOException e) {
            throw new com.qnct.common.BizException("文件保存失败：" + e.getMessage());
        }
    }

    /** 保存 Base64（mobile 拍照 / OCR） */
    public String saveBase64(String dir, String dataUrl) {
        int i = dataUrl.indexOf("base64,");
        String ext = dataUrl.startsWith("data:image/") ? dataUrl.substring(11, dataUrl.indexOf(';')) : "png";
        if (i < 0) {
            return save(dir, ext, dataUrl.getBytes());
        }
        return save(dir, ext, Base64.getDecoder().decode(dataUrl.substring(i + 7)));
    }
}