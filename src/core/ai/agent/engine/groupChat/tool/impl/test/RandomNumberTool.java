package ai.agent.engine.groupChat.tool.impl.test;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;

import java.util.Random;

@ToolDeclare(
    name = "RandomNumberTool",
    cnName = "随机数生成器",
    description = "生成指定范围内的随机数。可以指定最小值、最大值和生成数量",
    scope = ToolScope.GROUP_CHAT
)
public class RandomNumberTool extends AbsGroupChatTool {

    @ParamDeclare(description = "最小值", type = "number", required = false, defaultValue = "1")
    private int min;

    @ParamDeclare(description = "最大值", type = "number", required = false, defaultValue = "100")
    private int max;

    @ParamDeclare(description = "生成数量", type = "number", required = false, defaultValue = "1")
    private int count;

    private static final Random random = new Random();

    @Override
    protected String executeInternal() {
        if (min >= max) {
            return "错误: 最小值必须小于最大值";
        }

        if (count < 1 || count > 100) {
            return "错误: 生成数量必须在1-100之间";
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("生成 %d 个随机数 (范围: %d-%d):\n", count, min, max));

        for (int i = 0; i < count; i++) {
            int randomNum = min + random.nextInt(max - min + 1);
            result.append(randomNum);
            if (i < count - 1) {
                result.append(", ");
            }
        }

        return result.toString();
    }
}
