package chatroom;

import chatroom.common.Check;
import chatroom.common.Message;
import chatroom.common.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static chatroom.common.Constants.*;

public class ChatServer {
    private ServerSocket server;
    private ServerSocket fileServer;
    private User user;
    private Message formattedMessage;
    /**
     * key是用户名
     * User是用户名对应的信息
     */
    private Map<String, User> serverUserMap = new ConcurrentHashMap<>();

    private ExecutorService service = Executors.newCachedThreadPool();

    public ChatServer() throws Exception {
        server = new ServerSocket(SERVER_PORT);
        fileServer = new ServerSocket(FILE_PORT);
    }

    /**
     * 使用线程处理每个客户端传输的文件
     *
     * @throws Exception
     */
    public void load() throws Exception {
        while (true) {
            //接收连接服务端的客户端对象
            Socket socket = server.accept();
            System.out.println("已接收来自客户端的ip：" + socket.getRemoteSocketAddress());
            // 每接收到一个Socket就建立一个新的线程来处理它
            service.submit(new Transfer(socket));
        }
    }

    /**
     * 处理客户端传输过来的命令
     * 处理文件--文件线程
     */
    class Transfer implements Runnable {
        private Socket socket;
        private String userName = null;
        private String message;

        public Transfer(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            user = new User(socket);//默认为信息中from 的客户端
            formattedMessage = new Message();

            initUser(user);//初始化用户先

            while (true) {
                try {
                    message = user.receiveMessage();

                    //利用信息开头是否包含@来区分聊天信息和文件信息
                    if (!message.startsWith(SEND_FILE)) {
                        //如果不是传送文件就去处理命令
                        if (message.startsWith(CHAT_WITH_START)) {
                            Message get = formattedMessage.buildMessage(message);

                            //如果用户列表当中包含当前信息的用户信息，则将该用户设为当前使用用户
                            String currentName = get.getFrom().substring(1).trim();
                            //验证是不是命令
                            if (Check.isValidUserName(currentName) == null) {
                                if (serverUserMap.containsKey(currentName)) {
                                    this.userName = currentName;
                                    user = serverUserMap.get(userName);
                                }
                            }

                            if (get.getTo().equalsIgnoreCase(HEART_BRAT)) {
                                checkOnline(get.getFrom().substring(1).trim());
                                continue;//心跳检测不重置最后使用时间
                            }
                            handleMessage(message);
                            setLastUseTime();
                            continue;
                        }
                        setLastUseTime();
                    } else {
                        Thread fileTransfer = new Thread(() -> {
                            //处理文件，获取 目标用户名 和 发送用户名
                            Message get = formattedMessage.buildMessage(message);
                            String[] toInfo = get.getTo().split(SPACE_STRING);
                            String targetUser = toInfo[0];
                            String senderUser = toInfo[1];
                            if (!isOnline(targetUser)) {
                                serverUserMap.get(senderUser).sendMessage(new Message(CHAT_WITH_START + "系统信息", senderUser, "用户 " + targetUser + " 已离线"));
                            } else {
                                try (
                                        Socket fileSocket = fileServer.accept();
                                        DataInputStream dis = new DataInputStream(fileSocket.getInputStream());

                                ) {
                                    System.out.println("文件传输已接收来自客户端的ip：" + fileSocket.getRemoteSocketAddress());

                                    String fileName = dis.readUTF();
                                    long fileLength = dis.readLong();
                                    // 自定义文件夹存放上传的文件
                                    File directory = new File(FILE_STORAGE_PATH + File.separator + targetUser);
                                    if (!directory.exists()) {
                                        directory.mkdirs();
                                    }
                                    String filename = senderUser + "-" + fileName;
                                    File file = new File(directory.getAbsolutePath() + File.separatorChar + filename);
                                    try (
                                            FileOutputStream fos = new FileOutputStream(file);
                                    ) {
                                        // 开始接收文件
                                        byte[] bytes = new byte[1024];
                                        int length = 0;
                                        while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                                            fos.write(bytes, 0, length);
                                            fos.flush();
                                            serverUserMap.get(targetUser).sendMessage(new Message(SEND_FILE, targetUser + SPACE_STRING + filename, Arrays.toString(bytes)));
                                        }
                                    }

                                    serverUserMap.get(targetUser).sendMessage(new Message(CHAT_WITH_START + "系统信息", targetUser, "已接收来自用户" + senderUser + "的文件，" + "文件名：" + filename + " 大小：" + fileLength + "字节。"));
                                    serverUserMap.get(senderUser).sendMessage(new Message(CHAT_WITH_START + "系统信息", senderUser, "文件已成功发送给" + targetUser));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            serverUserMap.get(senderUser).setLastUseTime(Calendar.getInstance().getTime());
                        });
                        fileTransfer.start();
                    }
                } catch (Exception e) {
                    serverUserMap.get(userName).close();
                    serverUserMap.remove(userName);
                    System.out.println("已经移除" + userName);
                    e.printStackTrace();
                    return;
                }
            }
        }

        private void handleMessage(String message) {
            //将接受的命令转为对象方便调用
            Message get = formattedMessage.buildMessage(message);
            String to = get.getTo().substring(1);
            if (Check.isValidUserName(to) == null) {
                handleChat(get);//如果目标用户名合法的话-->不是命令
            } else {
                handleCommand(get);
            }
        }

        private void handleChat(Message message) {
            String from = message.getFrom().substring(1).trim();
            String to = message.getTo().substring(1).trim();
            User toUser = serverUserMap.get(to);

            if (toUser == null) {
                serverUserMap.get(from).sendMessage(new Message(CHAT_WITH_START + "系统信息", from, "用户名\"" + to + "\"不存在"));
            } else {
                toUser.sendMessage(message);
            }
        }

        private void handleCommand(Message message) {
            //传的message对象-->from:发命令的人  to:命令前缀  message:具体命令
            String from = message.getFrom().substring(1).trim();
            User toUser = serverUserMap.get(from);
            String command = message.getMessage().trim();
            String to = message.getTo().trim();

            if (command.equalsIgnoreCase(SERVER_COMMAND_LOGOFF)) {
                toUser.sendMessage(new Message(ADMIN_NAME, message.getFrom(), BYE));

                for (String toUsers : serverUserMap.keySet()) {
                    User toSend = serverUserMap.get(toUsers);//这里如果不创建新的对象，那么会一直发给自己
                    if (!toUsers.equalsIgnoreCase(from)) {//不是发送者就发出一条信息--不发给自己
                        toSend.sendMessage(new Message(CHAT_WITH_START + "系统信息", toUsers, "用户 " + from + " 离开了聊天室"));
                    }
                }
//                toUser.close();
                serverUserMap.remove(userName).close();
                this.userName = null;
                System.out.println("用户 \"" + from + "\" 离开了聊天室");
            } else if (command.equalsIgnoreCase(SERVER_COMMAND_LIST)) {
                String allNames = getAllNames();
                toUser.sendMessage(new Message(CHAT_WITH_START + "系统信息", CHAT_WITH_START + from, "所有在线用户:" + allNames));
            } else if (to.equalsIgnoreCase(SET_NAME)) {
                String newName = message.getMessage();

                serverUserMap.get(from).setUsername(newName);

                User oldValue = serverUserMap.remove(from);
                serverUserMap.put(newName, oldValue);
                this.userName = newName;
                serverUserMap.get(newName).sendMessage(new Message(CHAT_WITH_START + "系统信息", CHAT_WITH_START + newName, "原用户名 " + from + " 成功改为 " + newName));
            } else if (to.equalsIgnoreCase(ALL_USER)) {//群发功能
                Set<String> userList = serverUserMap.keySet();

                for (String toUsers : userList) {
                    User toSend = serverUserMap.get(toUsers);//这里如果不创建新的对象，那么会一直发给自己
                    if (!toUsers.equalsIgnoreCase(from)) {//不是发送者就发出一条信息--不发给自己
                        toSend.sendMessage(new Message(message.getFrom(), toUsers, message.getMessage()));
                    }
                }
            }
        }

        private void initUser(User user) {
            String errorMsg = null;
            while (true) {
                String allNames = getAllNames();
                user.sendMessage(new Message(ADMIN_NAME, NO_NAME, (errorMsg == null ? "" : "用户名非法，错误信息为" + errorMsg + ":") + "用户名不可以重复，现有的用户名有：[" + allNames + "]。请输入你的用户名："));

                Message chatMessage;
                try {
                    chatMessage = formattedMessage.buildMessage(user.receiveMessage());

                    String userName = chatMessage.getMessage();
                    errorMsg = Check.isValidUserName(userName);
                    if (errorMsg == null && (!serverUserMap.containsKey(userName))) {
                        Calendar now = Calendar.getInstance();
                        Date nowDate = now.getTime();
                        user.setUsername(userName);
                        user.setLoginTime(nowDate);
                        user.setLastUseTime(nowDate);

                        this.userName = userName;
                        serverUserMap.put(userName, user);
                        user.sendMessage(new Message(ADMIN_NAME, userName, USER_NAME_PASS));
                        user.sendMessage(new Message(ADMIN_NAME, userName, userName + COMMAND_INTRODUCTION));
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    user.close();
                    return;
                }
            }
        }

        private String getAllNames() {
            String userNameListSep = ",";
            if (serverUserMap.isEmpty()) {
                return ADMIN_NAME;
            } else {
                return ADMIN_NAME + userNameListSep + CHAT_WITH_START
                        + String.join(userNameListSep + CHAT_WITH_START, serverUserMap.keySet());
            }
        }

        private boolean isOnline(String userName) {
            return serverUserMap.containsKey(userName);
        }

        private void setLastUseTime() {
            //设置最后使用时间
            if (this.userName != null) {
                serverUserMap.get(userName).setLastUseTime(Calendar.getInstance().getTime());
            }
        }

        private void checkOnline(String userName) {
            if (this.userName != null) {
                if (this.userName.equals(userName)) {
                    User to = serverUserMap.get(userName);
                    double timer = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - to.getLastUseTime().getTime());
                    if (timer >= TimeUnit.MINUTES.toMinutes(TIMEOUT_RANGE)) {
                        to.sendMessage(new Message(HEART_BRAT, CHAT_WITH_START + userName, OFFLINE));
                        serverUserMap.remove(userName);
                        this.userName = null;
                    } else {
                        to.sendMessage(new Message(HEART_BRAT, CHAT_WITH_START + userName, ONLINE));
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // 开启服务端
            ChatServer server = new ChatServer();
            // 调用上传文件方法
            server.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
