package ai.agent.engine.groupChat.tool.impl.test;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@ToolDeclare(
    name = "GetCurrentTimeTool",
    cnName = "获取当前时间",
    description = "获取当前时间。可以指定时间格式(datetime/date/time/timestamp)和时区",
    scope = ToolScope.GROUP_CHAT
)
public class GetCurrentTimeTool extends AbsGroupChatTool {

    @ParamDeclare(
        description = "时间格式",
        enumValues = {"datetime", "date", "time", "timestamp"},
        required = false,
        defaultValue = "datetime"
    )
    private String format;

    @ParamDeclare(
        description = "时区，例如：Asia/Shanghai, UTC, America/New_York",
        required = false,
        defaultValue = "Asia/Shanghai"
    )
    private String timezone;

    @Override
    protected String executeInternal() {
        Date now = new Date();
        
        String result;
        switch (format.toLowerCase()) {
            case "datetime":
                result = formatDateTime(now, timezone);
                break;
            case "date":
                result = formatDate(now, timezone);
                break;
            case "time":
                result = formatTime(now, timezone);
                break;
            case "timestamp":
                result = String.valueOf(now.getTime());
                break;
            default:
                result = formatDateTime(now, timezone);
        }

        return String.format("当前时间 (%s): %s", timezone, result);
    }

    /**
     * 格式化为日期时间
     */
    private String formatDateTime(Date date, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.format(date);
    }

    /**
     * 格式化为日期
     */
    private String formatDate(Date date, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.format(date);
    }

    /**
     * 格式化为时间
     */
    private String formatTime(Date date, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.format(date);
    }
}
