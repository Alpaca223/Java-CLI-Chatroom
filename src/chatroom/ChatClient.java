package chatroom;

import chatroom.common.Check;
import chatroom.common.Message;
import chatroom.common.User;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static chatroom.common.Constants.*;

public class ChatClient {
    private Socket client;
    private String SERVER_IP;
    private File directory;
    private Scanner scanner = new Scanner(System.in);
    private User user;
    private User fileTransport;
    private Message message;
    private String currentUsername;
    private String lastChatUser;
    private Map<String, FileOutputStream> clientFileMap = new ConcurrentHashMap<>();

    //创建客户端，并指定接收的服务端IP和端口号
    public ChatClient(String SERVER_IP) throws IOException {
        this.SERVER_IP = SERVER_IP;
        this.client = new Socket(SERVER_IP, SERVER_PORT);
        System.out.println("连接服务端成功，服务端地址为：" + client.getRemoteSocketAddress());
        user = new User(this.client);
        message = new Message();
    }

    public void start() throws IOException {
        initName();//初始化用户

        Thread readThread = new Thread(() -> {
            while (true) {
                try {
                    Message chatMessage = message.buildMessage(user.receiveMessage());

                    if (chatMessage.getFrom().equalsIgnoreCase(ADMIN_NAME)
                            && chatMessage.getMessage().trim().equalsIgnoreCase(BYE)) {
                        user.close();
                        System.out.println("已经离开聊天室，程序结束");
                        System.exit(0);
                    } else if (chatMessage.getTo().toLowerCase().contains(currentUsername.toLowerCase())
                            || chatMessage.getTo().contains(NO_NAME)) {
                        String from = chatMessage.getFrom().substring(1).trim();
                        String to = chatMessage.getTo().trim();
                        String message = chatMessage.getMessage().trim();
                        if (message.contains(OFFLINE)) {
                            System.out.println(OFFLINE_NOTICE);
                            user.close();
                            System.exit(0);
                        } else if (!chatMessage.getFrom().equalsIgnoreCase(HEART_BRAT)) {
                            //发送不是心跳检测的消息
                            if (chatMessage.getFrom().equalsIgnoreCase(SEND_FILE)) {
                                String filename = to.substring(to.indexOf(SPACE_STRING) + 1);

                                directory = new File(FILE_STORAGE_PATH_CLIENT + File.separator + currentUsername);
                                if (!directory.exists()) {
                                    directory.mkdirs();
                                }

                                File file = new File(directory.getAbsolutePath() + File.separatorChar + filename);

                                try (
                                        FileOutputStream fos = new FileOutputStream(file, true);
                                ) {
                                    clientFileMap.put(filename, fos);//加入map当中
                                    byte[] ret = getByteArray(message);
                                    fos.write(ret, 0, ret.length);
                                    fos.flush();
                                    fos.close();
                                    continue;
                                }

                            }
                            System.out.println("form\"" + from + "\":" + message);
                            if (message.contains("已接收来自用户")) {
                                String[] ret = message.split("：");
                                String filename = ret[1].substring(0, ret[1].indexOf("大") - 1).trim();
                                if (clientFileMap.containsKey(filename)) {
                                    if (clientFileMap.size() > 1) {
                                        clientFileMap.remove(filename);
                                        System.out.println("文件保存在：" + directory.getAbsolutePath() + "\\" + filename);
                                        continue;
                                    }
                                    clientFileMap.get(filename).close();//收到相应的关闭
                                    clientFileMap.remove(filename);
                                }
                                System.out.println("文件保存在：" + directory.getAbsolutePath() + "\\" + filename);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    user.close();
                    System.exit(-2);
                }
            }

        });
        readThread.start();

        Thread writeThread = new Thread(() -> {
            //设置定时任务，心跳检测客户端是否还在连接上
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    user.sendMessage(new Message(CHAT_WITH_START + currentUsername, HEART_BRAT, HEART_BRAT));
                }
            }, 1000, TimeUnit.MINUTES.toMillis(INTERVAL));//10分钟检测一次,这里都是毫秒表示的


            while (true) {
                try {
                    //因为这个有输入检测，所以要放到后面
                    user.sendMessage(createMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    user.close();
                    System.exit(-3);
                }
            }
        });
        writeThread.start();
    }

    /**
     * 把传过来的字符串数据转换为byte数组
     *
     * @param string 传的字符串
     * @return byte[]
     */
    private byte[] getByteArray(String string) {
        String format = string.substring(1, (string.length() - 1));
        String[] arr = format.split(",");
        byte[] bytes = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            bytes[i] = Byte.parseByte(arr[i].trim());
        }
        return bytes;
    }

    private Message createMessage() {
        while (true) {
            String line = scanner.nextLine().trim();
            String to = null;
            String message = null;
            if (line.startsWith(CHAT_WITH_START)) {
                to = line.substring(0, line.indexOf(SPACE_STRING)).trim();

                if (line.startsWith(SEND_FILE)) {
                    try (
                            Socket fileClient = new Socket(SERVER_IP, FILE_PORT);
                    ) {
                        System.out.println("连接文件传输服务端成功，服务端地址为：" + fileClient.getRemoteSocketAddress());
                        fileTransport = new User(fileClient);

                        String[] strings = line.split(SPACE_STRING);
                        StringBuilder path = new StringBuilder();
                        for (int i = 2; i < strings.length; i++) {
                            path.append(strings[i]).append(SPACE_STRING);
                        }
                        //to包含的信息为：目标用户 发送者
                        user.sendMessage(new Message(strings[0], strings[1] + SPACE_STRING + currentUsername, path.toString().trim()));
                        sendFile(path.toString().trim());
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    lastChatUser = to;
                    message = line.substring(line.indexOf(SPACE_STRING) + 1);
                    if (line.startsWith(SET_NAME)) {//改名的特殊情况
                        String[] strings = line.split(SPACE_STRING);
                        user.sendMessage(new Message(CHAT_WITH_START + currentUsername, strings[0], strings[1]));
                        currentUsername = message;//发完再变
                        continue;
                    }
                }
            } else {
                if (lastChatUser == null) {
                    System.out.println("请使用@输入想和谁聊天，以后如果和同一个人聊天，就可以不用@啦");
                    continue;
                }
                if (lastChatUser.startsWith(CHAT_WITH_START)) {
                    lastChatUser = lastChatUser.substring(1);//去掉@再去验证
                }
                String error = Check.isValidUserName(lastChatUser);
                if (error != null) {
                    System.out.println("上一个聊天的用户名为 " + lastChatUser + " 不合法：" + error);
                    lastChatUser = null;
                    continue;
                }
                lastChatUser = CHAT_WITH_START + lastChatUser;//验证完了合法再加回去

                message = line;
            }
            //来自当前用户，发给上一个聊天的用户
            return new Message(CHAT_WITH_START + currentUsername, lastChatUser, message);
        }
    }


    //向服务端传输文件
    private void sendFile(String fileName) throws IOException {
        File file = new File(fileName);
        try (
                FileInputStream fis = new FileInputStream(file);
                DataOutputStream dos = new DataOutputStream(fileTransport.getSocket().getOutputStream());
        ) {
            dos.writeUTF(file.getName());
            dos.flush();
            dos.writeLong(file.length());
            dos.flush();
            byte[] bytes = new byte[1024];
            int length = 0;

            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                dos.write(bytes, 0, length);
                dos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("文件传输异常");
        }
    }

    private void initName() throws IOException {
        //把输入的名字暂存到这
        String typedName = null;

        while (true) {
            Message chatMessage = message.buildMessage(user.receiveMessage());
            String serverMessage = chatMessage.getMessage();
            //如果获取的数据为服务器发的UserNamePass，则通过
            if (serverMessage.equalsIgnoreCase(USER_NAME_PASS)) {
                currentUsername = typedName;//将当前用户赋值输入的用户名
                break;
            } else {
                System.out.println(serverMessage);//提示重新输入非重复名
                typedName = scanner.nextLine();
                String username = typedName.substring(typedName.indexOf(SPACE_STRING) + 1).trim();
                user.sendMessage(new Message("", "", username));
            }
        }
    }

    public static void main(String[] args) {
        try {
            // 启动客户端的连接
            ChatClient client = new ChatClient("localhost");
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
