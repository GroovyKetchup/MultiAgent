package ai.agent.dto.groupChat.workCache;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-08", updateTime = "2026-01-08"
)
public class WorkCachePanelDeliverStatusDto implements Serializable {

   public static final String DELIVER_TYPE_PROTOTYPE = "原型";
   public static final String DELIVER_TYPE_STANDARD = "标准";
   public static final String DELIVER_TYPE_PERSONAL = "个性";

   public static final String DELIVER_STATUS_PENDING = "待交付";
   public static final String DELIVER_STATUS_READY = "已就绪";
   public static final String DELIVER_STATUS_REQUIRE_RECTIFY = "需整改";

   public static final String TEST_STATUS_PENDING = "待测试";
   public static final String TEST_STATUS_READY = "已达标";
   public static final String TEST_STATUS_NOT_READY = "未达标";


    // 交付类型：原型、标准、个性
    private String deliverType;
    // 交付状态：待交付、已就绪、需整改
    private String deliverStatus;
    // 测试状态：待测试、已达标、未达标
    private String testStatus;


    public String getDeliverType() {
        return deliverType;
    }

    public WorkCachePanelDeliverStatusDto setDeliverType(String deliverType) {
        this.deliverType = deliverType;
        return this;
    }

    public String getDeliverStatus() {
        return deliverStatus;
    }

    public WorkCachePanelDeliverStatusDto setDeliverStatus(String deliverStatus) {
        this.deliverStatus = deliverStatus;
        return this;
    }

    public String getTestStatus() {
        return testStatus;
    }

    public WorkCachePanelDeliverStatusDto setTestStatus(String testStatus) {
        this.testStatus = testStatus;
        return this;
    }
}
