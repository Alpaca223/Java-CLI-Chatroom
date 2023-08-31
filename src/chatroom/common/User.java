package chatroom.common;

import java.io.*;
import java.net.Socket;
import java.util.Date;

import static chatroom.common.Constants.*;

public class User {
    private String username;//用户名
    private Socket socket;//用户的连接
    private Date loginTime;//登录时间
    private Date lastUseTime;//最后一次使用的时间
    private BufferedReader reader;
    private PrintWriter writer;

    public User(Socket socket) {
        this.socket = socket;
        try {
            init(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User(String username, Socket socket, Date loginTime, Date lastUseTime) {
        this.username = username;
        this.socket = socket;
        this.loginTime = loginTime;
        this.lastUseTime = lastUseTime;
        try {
            init(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init(Socket socket) throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), DEFAULT_CHARSET));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), DEFAULT_CHARSET));
    }

    public void sendMessage(Message message) {
        //这里是将格式化的数据通过socket传输
        writer.println(message.toMessageString());
        writer.flush();
    }

    public String receiveMessage() throws IOException {
        //通过socket接收传输的数据
        String line = null;
        while (true) {
            line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                return line;
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

    public Date getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(Date lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", socket=" + socket +
                ", loginTime=" + loginTime +
                ", lastUseTime=" + lastUseTime +
                '}';
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
