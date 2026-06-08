package ai.agent;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.http.HttpUtil;
import panelx.dto.agentforge.AgentForgeSettingDto;
import panelx.dto.agentforge.AgentForgeUserInfoDto;
import panelx.utils.AgentForgeJWTUtil;

import static cell.ai.agent.IAgentExperienceService.REMOTE_URL_SAVE;

public class Main {

    public static void main(String[] args) throws Exception {


//        Credential cred = SmsTencentUtil.getCredential(null, null);
//        SmsTencentUtil.send(cred, null, null, null, null,
//                "2521750", new String[]{}, "19050654025");


        System.out.println(REMOTE_URL_SAVE);

        String result = HttpUtil.post(REMOTE_URL_SAVE, "{'agentId':'123'}");

        System.out.println(result);


//        createJWT();


    }

    private static void createJWT() {
        DateTime now = DateTime.now();
        DateTime nextMonthTime = now.setField(DateField.MONTH, now.getField(DateField.MONTH) + 1);
        AgentForgeSettingDto settingDtO = new AgentForgeSettingDto()
                .setUser(new AgentForgeUserInfoDto()
                        .setUserName("CK")
                        .setEnterpriseName("公司内部")
                        .setPhone("19050654025")
                )
                .setMaxGPTChatLimit(2000)
                .setMaxSessions(2)
                .setExpireTime(nextMonthTime.getTime())
//                .setUrl("http://14.116.200.92:14289/AgentForge/");
                .setUrl("http://14.18.100.250:14089/AgentForge/");
//                .setUrl("https://demo.kwaidoo.com/test0916/AgentForge/");
//                .setUrl("https://demo.kwaidoo.com/kdcsx4/AgentForge/");
        String token = AgentForgeJWTUtil.generateToken(settingDtO, "kd2025");

        System.out.println(token);

//String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJtYXhTZXNzaW9ucyI6MywibWF4R1BUQ2hhdExpbWl0IjoxMDAsInVzZXIiOiIxODgxOTgwNDExNyJ9.xGnNBN3m73gigv7_3BDo3lmZzdbwkaTiqoJOmHRMDWA";
//        AgentForgeSettingDto test = AgentForgeJWTUtil.parseTokenToDTO(token, "kd2025");
//
//        System.out.println(JSONUtil.toJsonStr(test));
    }


}

