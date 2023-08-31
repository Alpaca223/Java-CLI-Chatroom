package chatroom.common;

import static chatroom.common.Constants.*;

public class Check {
    //检查用户名是否包含关键字，如果包含则让用户重新创建
    public static String isValidUserName(String userName) {
        String ret = isValidToUserName(userName);
        if (ret != null) {
            return ret;
        }

        if (userName.toLowerCase().contains(ADMIN_NAME)) {
            return "用户名不能包含关键字" + ADMIN_NAME;
        }

        if (userName.toLowerCase().contains(ALL_USER)) {
            return "用户名不能包含关键字" + ALL_USER;
        }

        if (userName.toLowerCase().contains(SEND_FILE)) {
            return "用户名不能包含关键字" + SEND_FILE;
        }

        if (userName.contains(SET_NAME)) {
            return "用户名不能包含关键字" + SET_NAME;
        }

        if (userName.contains(HEART_BRAT)) {
            return "用户名不能包含关键字" + HEART_BRAT;
        }

        for (String x : invalidUsername) {
            if (userName.equals(x)) {
//                System.out.println(x);
                return "请重新输入@用户名跟某个用户聊天";
            }
        }

        return null;
    }

    private static String isValidToUserName(String userName) {
        if (getNormalizedUserName(userName).isEmpty()) {
            return "用户名不能为空";
        }
        if (userName.contains(MESSAGE_SEP_STR)) {
            return "用户名不可以包含分隔符";
        }
        if (userName.contains(CHAT_WITH_START)) {
            return "用户名不可以包含" + CHAT_WITH_START;
        }
        if (userName.toLowerCase().contains(NO_NAME)) {
            return "用户名不可以包含" + NO_NAME;
        }
        if (userName.contains(SPACE_STRING)) {
            return "用户名不可以包含空格";
        }
        return null;
    }

    public static String getNormalizedUserName(String userName) {
        return userName.trim();
    }
}
