package ai.agent.engine.groupChat.tool.impl.test;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;

@ToolDeclare(
    name = "MathCalculatorTool",
    cnName = "数学计算器",
    description = "执行基本的数学运算。支持加法(add)、减法(subtract)、乘法(multiply)、除法(divide)",
    scope = ToolScope.GROUP_CHAT
)
public class MathCalculatorTool extends AbsGroupChatTool {

    @ParamDeclare(description = "第一个数字", type = "number", required = true)
    private double num1;

    @ParamDeclare(description = "第二个数字", type = "number", required = true)
    private double num2;

    @ParamDeclare(
        description = "运算符", 
        enumValues = {"add", "subtract", "multiply", "divide"},
        required = true
    )
    private String operator;

    @Override
    protected String executeInternal() {
        double result;
        String operatorSymbol;

        switch (operator.toLowerCase()) {
            case "add":
                result = num1 + num2;
                operatorSymbol = "+";
                break;
            case "subtract":
                result = num1 - num2;
                operatorSymbol = "-";
                break;
            case "multiply":
                result = num1 * num2;
                operatorSymbol = "×";
                break;
            case "divide":
                if (num2 == 0) {
                    return "错误: 除数不能为零";
                }
                result = num1 / num2;
                operatorSymbol = "÷";
                break;
            default:
                return "错误: 不支持的运算符 '" + operator + "'";
        }

        return String.format("计算结果: %.2f %s %.2f = %.2f", num1, operatorSymbol, num2, result);
    }
}
