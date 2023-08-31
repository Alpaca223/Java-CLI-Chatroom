package chatroom.common;

import static chatroom.common.Constants.*;

public class Message {
    private String from;
    private String to;
    private String message;

    public Message() {
    }

    public Message(String from, String to, String message) {
        this.from = from;
        this.to = to;
        this.message = message;
    }

    public String toMessageString() {
        //把对象转换为字符串
        return from + MESSAGE_SEP + to + MESSAGE_SEP + message + MESSAGE_BREAK;
    }

    public Message buildMessage(String message) {
        //把转化的字符串重新打包成对象
        Message ret = new Message();
        int first = message.indexOf(MESSAGE_SEP);
        int second = message.indexOf(MESSAGE_SEP, first + 1);

        ret.from = message.substring(0, first);
        ret.to = message.substring(first + 1, second);
        ret.message = message.substring(second + 1).trim();
        return ret;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
