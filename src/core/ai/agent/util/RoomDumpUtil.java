package ai.agent.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.kwaidoo.ms.tool.ToolUtilities;
import gpf.exception.VerifyException;
import org.nutz.dao.entity.annotation.Comment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

@Comment("房间（业务域）Dump导入导出工具类")
@ClassDeclare(
        label = "RoomDump工具",
        what = "提供业务域dump包的解析、导入和导出功能",
        why = "统一封装dump包的zip解析逻辑，简化导入导出操作",
        how = "解压外层zip包，读取domain.meta获取domainCode，提取payload.zip进行实际导入导出",
        developer = "裴硕", version = "1.0",
        createTime = "2026-02-24", updateTime = "2026-02-24"
)
public class RoomDumpUtil {

    public static final String FILE_DOMAIN_META = "domain.meta";
    public static final String FILE_PAYLOAD_ZIP = "payload.zip";
    public static final String FILE_ROOM_DATA = "room.data";
    public static final String FILE_ENGINE_DATA = "engine.data";
    public static final String TEMP_DIR_PREFIX = "./temp/RoomDump_";

    /**
     * 解析业务域dump包，提取出domainCode和payload.zip的字节内容，以及可选的room.data和engine.data
     *
     * dump包结构：
     * ├── domain.meta    (txt文件，内容为domainCode)
     * ├── payload.zip    (实际的数据压缩包)
     * ├── room.data      (可选，房间Form的JSON字符串)
     * └── engine.data    (可选，引擎数据JSON字符串)
     *
     * @param dumpZipBytes 业务域dump包的zip字节数组
     * @return 解析结果
     * @throws Exception 解析异常
     */
    public static DumpPackage parseDumpPackage(byte[] dumpZipBytes) throws Exception {
        if (dumpZipBytes == null || dumpZipBytes.length == 0) {
            throw new VerifyException("dump包文件为空");
        }

        File tempDir = FileUtil.mkdir(TEMP_DIR_PREFIX + ToolUtilities.allockUUIDWithUnderline());
        try {
            // 解压外层zip包
            try (ByteArrayInputStream bis = new ByteArrayInputStream(dumpZipBytes)) {
                ZipUtil.unzip(bis, tempDir, StandardCharsets.UTF_8);
            }

            // 读取domain.meta获取domainCode
            File domainMetaFile = new File(tempDir, FILE_DOMAIN_META);
            if (!domainMetaFile.exists()) {
                throw new VerifyException(StrUtil.format("dump包中未找到{}文件", FILE_DOMAIN_META));
            }
            String domainCode = FileUtil.readUtf8String(domainMetaFile).trim();
            if (StrUtil.isBlank(domainCode)) {
                throw new VerifyException(StrUtil.format("{}文件内容为空，无法获取domainCode", FILE_DOMAIN_META));
            }

            // 读取payload.zip
            File payloadZipFile = new File(tempDir, FILE_PAYLOAD_ZIP);
            if (!payloadZipFile.exists()) {
                throw new VerifyException(StrUtil.format("dump包中未找到{}文件", FILE_PAYLOAD_ZIP));
            }
            byte[] payloadBytes = FileUtil.readBytes(payloadZipFile);

            // 可选：读取room.data（房间Form JSON）
            String roomDataJson = null;
            File roomDataFile = new File(tempDir, FILE_ROOM_DATA);
            if (roomDataFile.exists()) {
                String content = FileUtil.readUtf8String(roomDataFile).trim();
                if (StrUtil.isNotBlank(content)) {
                    roomDataJson = content;
                }
            }

            // 可选：读取engine.data（引擎数据JSON字符串）
            String engineDataJson = null;
            File engineDataFile = new File(tempDir, FILE_ENGINE_DATA);
            if (engineDataFile.exists()) {
                String content = FileUtil.readUtf8String(engineDataFile).trim();
                if (StrUtil.isNotBlank(content)) {
                    engineDataJson = content;
                }
            }

            return new DumpPackage(domainCode, payloadBytes, roomDataJson, engineDataJson);

        } finally {
            FileUtil.del(tempDir);
        }
    }

    // ========================= 内部类定义 =========================

    /**
     * dump包解析结果
     */
    public static class DumpPackage {
        /**
         * 业务域编码
         */
        private final String domainCode;

        /**
         * payload.zip的字节内容
         */
        private final byte[] payloadBytes;

        /**
         * 房间Form的JSON字符串（可选，来自room.data）
         */
        private final String roomDataJson;

        /**
         * 引擎数据JSON字符串（可选，来自engine.data）
         */
        private final String engineDataJson;

        public DumpPackage(String domainCode, byte[] payloadBytes, String roomDataJson, String engineDataJson) {
            this.domainCode = domainCode;
            this.payloadBytes = payloadBytes;
            this.roomDataJson = roomDataJson;
            this.engineDataJson = engineDataJson;
        }

        public String getDomainCode() {
            return domainCode;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public String getRoomDataJson() {
            return roomDataJson;
        }

        public String getEngineDataJson() {
            return engineDataJson;
        }

        public boolean hasRoomData() {
            return StrUtil.isNotBlank(roomDataJson);
        }

        public boolean hasEngineData() {
            return StrUtil.isNotBlank(engineDataJson);
        }
    }
}
