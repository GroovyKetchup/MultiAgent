package ai.agent.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import gpf.exception.VerifyException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Comment("前端包管理工具类")
@ClassDeclare(
        label = "WebApp工具",
        what = "提供前端包的管理功能，包括列出、检查、重命名、移除、上传前端包",
        why = "统一管理./webapps目录下的前端包，简化前端包的部署和维护",
        how = "通过文件系统操作和ZIP解压实现前端包的增删改查",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-13", updateTime = "2026-01-13"
)
public class WebAppUtil {

    /**
     * 前端包根目录
     */
    public static final String WEBAPPS_ROOT = "./webapps/";

    /**
     * 获取webapps根目录File对象
     */
    private static File getWebAppsRoot() {
        return new File(WEBAPPS_ROOT);
    }

    /**
     * 确保webapps目录存在
     */
    private static void ensureWebAppsRootExists() {
        File root = getWebAppsRoot();
        if (!root.exists()) {
            root.mkdirs();
        }
    }

    // ========================= 核心功能方法 =========================

    /**
     * 列出所有已有的前端包
     *
     * @return 前端包信息列表
     */
    public static List<WebAppInfo> listWebApps() {
        List<WebAppInfo> result = new ArrayList<>();
        File root = getWebAppsRoot();

        if (!root.exists() || !root.isDirectory()) {
            return result;
        }

        File[] files = root.listFiles();
        if (files == null) {
            return result;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                WebAppInfo info = new WebAppInfo();
                info.setName(file.getName());
                info.setPath(file.getAbsolutePath());
                info.setSize(FileUtil.size(file));
                info.setLastModified(file.lastModified());
                result.add(info);
            }
        }

        return result;
    }

    /**
     * 检查指定名称的前端包是否存在
     *
     * @param appName 前端包名称
     * @return true-存在，false-不存在
     */
    public static boolean exists(String appName) {
        if (StrUtil.isBlank(appName)) {
            return false;
        }
        File appDir = new File(getWebAppsRoot(), appName);
        return appDir.exists() && appDir.isDirectory();
    }

    /**
     * 重命名前端包
     *
     * @param oldName 原名称
     * @param newName 新名称
     * @throws VerifyException 参数校验异常
     */
    public static void rename(String oldName, String newName) throws VerifyException {
        validateAppName(oldName, "原名称");
        validateAppName(newName, "新名称");

        File oldDir = new File(getWebAppsRoot(), oldName);
        File newDir = new File(getWebAppsRoot(), newName);

        if (!oldDir.exists()) {
            throw new VerifyException(StrUtil.format("前端包[{}]不存在", oldName));
        }

        if (newDir.exists()) {
            throw new VerifyException(StrUtil.format("目标名称[{}]已存在", newName));
        }

        boolean success = oldDir.renameTo(newDir);
        if (!success) {
            throw new VerifyException(StrUtil.format("重命名前端包[{}]到[{}]失败", oldName, newName));
        }
    }

    /**
     * 移除前端包
     *
     * @param appName 前端包名称
     * @throws VerifyException 参数校验异常
     */
    public static void remove(String appName) throws VerifyException {
        validateAppName(appName, "前端包名称");

        File appDir = new File(getWebAppsRoot(), appName);

        if (!appDir.exists()) {
            throw new VerifyException(StrUtil.format("前端包[{}]不存在", appName));
        }

        try {
            FileUtil.del(appDir);
        } catch (Exception e) {
            throw new VerifyException(StrUtil.format("删除前端包[{}]失败: {}", appName, e.getMessage()));
        }
    }


    /**
     * 上传前端包（接收zip文件的byte[] + 指定appName）
     * 
     * 支持两种zip包结构：
     * 1. 带顶级目录：CDP.zip -> CDP/index.html
     * 2. 不带顶级目录：CDP.zip -> index.html
     *
     * @param zipBytes zip文件字节数组
     * @param appName  指定的前端包名称（必填）
     * @return 操作结果信息
     * @throws Exception 上传异常
     */
    public static String upload(byte[] zipBytes, String appName) throws Exception {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new VerifyException("上传的zip文件为空");
        }
        validateAppName(appName, "前端包名称");

        ensureWebAppsRootExists();
        File webAppsRoot = getWebAppsRoot();

        // 删除目标目录（如果存在）
        File targetAppDir = new File(webAppsRoot, appName);
        if (targetAppDir.exists()) {
            FileUtil.del(targetAppDir);
        }

        // 判断zip包结构类型
        ZipStructureType structureType = analyzeZipStructure(zipBytes);
        Charset zipCharset = structureType.charset;

        // 根据结构类型解压
        if (structureType.hasTopLevelDir && structureType.topLevelDirName.equals(appName)) {
            // 情况1：zip有顶级目录且与appName相同，解压到webapps根目录
            try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes)) {
                ZipUtil.unzip(bis, webAppsRoot, zipCharset);
            }
        } else {
            // 情况2：zip没有顶级目录，或顶级目录与appName不同，解压到目标目录内
            if (!targetAppDir.exists()) {
                targetAppDir.mkdirs();
            }
            try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes)) {
                ZipUtil.unzip(bis, targetAppDir, zipCharset);
            }
        }

        return StrUtil.format("前端包[{}]部署成功", appName);
    }

    /**
     * 备份并上传前端包（安全更新策略）
     * 策略：先备份现有前端包，再上传新的前端包
     * 
     * 支持两种zip包结构：
     * 1. 带顶级目录：CDP.zip -> CDP/index.html
     * 2. 不带顶级目录：CDP.zip -> index.html
     *
     * @param zipBytes    zip文件字节数组
     * @param webAppName  前端包名称
     * @return 操作结果信息
     * @throws Exception 上传异常
     */
    public static String backupAndUpload(byte[] zipBytes, String webAppName) throws Exception {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new VerifyException("上传的zip文件为空");
        }
        validateAppName(webAppName, "前端包名称");

        ensureWebAppsRootExists();
        File webAppsRoot = getWebAppsRoot();

        // 1. 备份现有前端包（如果存在）
        String backupName = null;
        if (exists(webAppName)) {
            backupName = generateBackupName(webAppName);
            rename(webAppName, backupName);
        }
        ZipStructureType structureType = null;

        try {
            // 2. 判断zip包结构类型
            structureType = analyzeZipStructure(zipBytes);
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e);
        }

        // 3. 根据结构类型解压
        File targetAppDir = new File(webAppsRoot, webAppName);
        Charset zipCharset = structureType.charset;
        
        if (structureType.hasTopLevelDir && structureType.topLevelDirName.equals(webAppName)) {
            // 情况1：zip有顶级目录且与webAppName相同，解压到webapps根目录
            try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes)) {
                ZipUtil.unzip(bis, webAppsRoot, zipCharset);
            }
        } else {
            // 情况2：zip没有顶级目录，或顶级目录与webAppName不同，解压到目标目录内
            if (!targetAppDir.exists()) {
                targetAppDir.mkdirs();
            }
            try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes)) {
                ZipUtil.unzip(bis, targetAppDir, zipCharset);
            }
        }

        // 返回结果
        if (backupName != null) {
            return StrUtil.format("前端包[{}]更新成功，原包已备份为[{}]", webAppName, backupName);
        } else {
            return StrUtil.format("前端包[{}]部署成功（首次部署，无需备份）", webAppName);
        }
    }
    
    /**
     * 分析zip包结构类型
     * 判断zip是否有唯一的顶级目录
     */
    private static ZipStructureType analyzeZipStructure(byte[] zipBytes) throws Exception {
        try {
            return analyzeZipStructureWithCharset(zipBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // UTF-8 解码失败（通常是 MALFORMED 错误），fallback 到 GBK
            return analyzeZipStructureWithCharset(zipBytes, Charset.forName("GBK"));
        }
    }

    private static ZipStructureType analyzeZipStructureWithCharset(byte[] zipBytes, Charset charset) throws Exception {
        Set<String> topLevelNames = new HashSet<>();
        String topLevelDirName = null;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bis, charset)) {

            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String entryName = ze.getName();
                if (entryName == null || entryName.trim().isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                // 规范化分隔符
                String normalized = entryName.replace("\\", "/");
                String[] parts = normalized.split("/");

                if (parts.length > 0 && StrUtil.isNotBlank(parts[0])) {
                    topLevelNames.add(parts[0]);

                    // 检查是否是目录（以/结尾或有子路径）
                    if (ze.isDirectory() || parts.length > 1) {
                        if (topLevelDirName == null) {
                            topLevelDirName = parts[0];
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        // 判断：如果只有一个顶级名称，且它是目录，则认为有顶级目录
        ZipStructureType result = new ZipStructureType();
        result.charset = charset;
        if (topLevelNames.size() == 1 && topLevelDirName != null) {
            result.hasTopLevelDir = true;
            result.topLevelDirName = topLevelDirName;
        } else {
            result.hasTopLevelDir = false;
            result.topLevelDirName = null;
        }

        return result;
    }
    
    /**
     * zip包结构类型
     */
    private static class ZipStructureType {
        boolean hasTopLevelDir;
        String topLevelDirName;
        Charset charset = StandardCharsets.UTF_8;
    }

    /**
     * 生成备份名称
     * 格式：backup_时间戳_原名称
     */
    private static String generateBackupName(String webAppName) {
        long timestamp = System.currentTimeMillis();
        return StrUtil.format("backup_{}_{}", timestamp, webAppName);
    }

    // ========================= 辅助方法 =========================

    /**
     * 校验前端包名称
     */
    private static void validateAppName(String appName, String fieldName) throws VerifyException {
        if (StrUtil.isBlank(appName)) {
            throw new VerifyException(StrUtil.format("{}不能为空", fieldName));
        }

        // 检查是否包含非法字符
        if (appName.contains("..") || appName.contains("/") || appName.contains("\\")) {
            throw new VerifyException(StrUtil.format("{}包含非法字符", fieldName));
        }
    }

    // ========================= 内部类定义 =========================

    /**
     * 前端包信息
     */
    public static class WebAppInfo {
        private String name;
        private String path;
        private long size;
        private long lastModified;

        public String getName() {
            return name;
        }

        public WebAppInfo setName(String name) {
            this.name = name;
            return this;
        }

        public String getPath() {
            return path;
        }

        public WebAppInfo setPath(String path) {
            this.path = path;
            return this;
        }

        public long getSize() {
            return size;
        }

        public WebAppInfo setSize(long size) {
            this.size = size;
            return this;
        }

        public long getLastModified() {
            return lastModified;
        }

        public WebAppInfo setLastModified(long lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        public String toString() {
            return StrUtil.format("WebAppInfo[name={}, path={}, size={}, lastModified={}]",
                    name, path, size, lastModified);
        }
    }
}
