package chatroom.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Constants {
    int SERVER_PORT = 8999;
    int FILE_PORT = 9000;
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    char MESSAGE_SEP = Character.UNASSIGNED;//0
    String SPACE_STRING = " ";
    String MESSAGE_SEP_STR = String.valueOf(Character.UNASSIGNED);
    String ADMIN_NAME = "@admin";
    String ALL_USER = "@all";
    String USER_NAME_PASS = "UserNamePass";
    String COMMAND_INTRODUCTION =
            "欢迎来到聊天室，你可以" +
                    "使用@setName 名称-->来修改昵称，" +
                    "使用@admin list-->来查看所有的在线用户，" +
                    "使用@用户名跟某个用户聊天，如果和同一个用户聊天，后续的消息则无需再次输入@，" +
                    "使用@admin logoff-->来离开聊天室，" +
                    "使用@all 消息-->来群发信息，" +
                    "使用@file 目标用户 文件全路径-->来发送文件，" +
                    "超过30分钟未使用会强制下线";
    String CHAT_WITH_START = "@";
    String SEND_FILE = "@file";
    String SET_NAME = "@setName";
    String NO_NAME = "anonymous";
    String SERVER_COMMAND_LOGOFF = "logoff";
    String SERVER_COMMAND_LIST = "list";
    String BYE = "bye";
    String MESSAGE_BREAK = "\n";
    String[] invalidUsername = {"admin", "all", "file", "setName", "heartBeat"};
    String HEART_BRAT = "@heartBeat";
    String ONLINE = "online";
    String OFFLINE = "offline";
    String OFFLINE_NOTICE = "由于您超过30分钟未操作，已被系统强制下线！请重启客户端";
    Long INTERVAL = 10L;//客户端心跳检测间隔--分钟
    Long TIMEOUT_RANGE = 30L;
    String FILE_STORAGE_PATH = "userFile/server";
    String FILE_STORAGE_PATH_CLIENT = "userFile";
}
