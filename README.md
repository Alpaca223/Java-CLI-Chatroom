# Java-CLI-Chatroom
一个使用Java语言编写的CLI聊天室，主要用到了Socket、IO、线程池

1.可以进入聊天室，根据系统提示输入用户名。系统会检查用户名是否重复，是否包含关键词，如包含则需要重新输入  
2.服务器端可以进行消息转发，输入的消息仅限文本。服务端会进行心跳检测，会断开长时间未使用的客户端连接  
3.可以查看在线用户列表:@admin list  
4.可以和在线用户私聊：@用户名跟某个用户聊天，如果和同一个用户聊天，后续的消息则无需再次输入@  
5.可以进行群发消息（消息内容仅限文本）：@all 消息  
6.可以发送文件：@file 目标用户 文件全路径（如C:\Users\xxx\Desktop\xxx.txt）,接收的文件默认保存在项目的userFile路径下  
7.可以修改昵称：@setName 名称  
8.可以退出聊天室：@admin logoff