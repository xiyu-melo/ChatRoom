package com.example.demo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

@ServerEndpoint("/websocket/{sid}/{userName}")
@Component
public class WebSocketServer {
	
	static org.slf4j.Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    public static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    //接收sid
    private String sid="";
    
    private String userName = "";
    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session,@PathParam("sid") String sid,@PathParam("userName") String userName) {
        this.session = session;
        this.sid=sid;
        this.userName=userName;
        
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        log.info("有新窗口开始监听:"+sid+",当前在线人数为" + getOnlineCount());
        
        try {
        	 sendMessage(sessionList());
        	 refreshOtherSessionList();
        } catch (IOException e) {
            log.error("websocket IO异常");
        }
    }

    private String sessionList() {
    	List<WebSocketServer> collect = webSocketSet.stream().filter(w->!w.getSid().equals(this.sid)).collect(Collectors.toList());
    	
    	SendMsgModel sendMsgModel = new SendMsgModel();
		sendMsgModel.setMsg(JSON.toJSONString(collect));
		sendMsgModel.setSourceSid(this.getSid());
		sendMsgModel.setType(MsgTypeEnum.USER_LIST.getCode());
    	return JSON.toJSONString(sendMsgModel);
    }
    
    private void refreshOtherSessionList() {
    	webSocketSet.stream().filter(w->!w.getSid().equals(this.sid)).forEach(w->{
    		
    		List<WebSocketServer> msg = webSocketSet.stream().filter(s->!s.getSid().equals(w.getSid())).collect(Collectors.toList());
    		SendMsgModel sendMsgModel = new SendMsgModel();
    		sendMsgModel.setMsg(JSON.toJSONString(msg));
    		sendMsgModel.setSourceSid(this.getSid());
    		sendMsgModel.setType(MsgTypeEnum.USER_LIST.getCode());
    		
    		try {
				sendInfo(JSON.toJSONString(sendMsgModel), w.getSid());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	});
    }
    
    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message, Session session) {
    	log.info("收到来自窗口"+sid+"的信息:"+message);
        
        AcceptMsgModel msgModel = JSON.parseObject(message,AcceptMsgModel.class);
        Integer type = msgModel.getType();
        if(type.equals(MsgTypeEnum.MSG_SINGLE.getCode())) {
        	webSocketSet.stream().filter(w->w.getSid().equals(msgModel.getSendSid())).forEach(w->{
				try {
					
					SendMsgModel sendNodel = new SendMsgModel();
					sendNodel.setMsg(msgModel.getMsg());
					sendNodel.setType(MsgTypeEnum.MSG_SINGLE.getCode());
					sendNodel.setSourceSid(this.getSid());
					w.sendMessage(JSON.toJSONString(sendNodel));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
        }else if(type.equals(MsgTypeEnum.MSG_ALL.getCode())) {
			SendMsgModel sendNodel = new SendMsgModel();
			sendNodel.setType(MsgTypeEnum.MSG_ALL.getCode());
			sendNodel.setSourceSid(this.getSid());
			sendNodel.setMsg(msgModel.getMsg());
			String str = JSON.toJSONString(sendNodel);
        	
        	for (WebSocketServer item : webSocketSet) {
                try {
                    item.sendMessage(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

	/**
	 * 
	 * @param session
	 * @param error
	 */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        error.printStackTrace();
    }
	/**
	 * 实现服务器主动推送
	 */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 群发自定义消息
     * */
    public static void sendInfo(String message,@PathParam("sid") String sid) throws IOException {
    	log.info("推送消息到窗口"+sid+"，推送内容:"+message);
        for (WebSocketServer item : webSocketSet) {
            try {
            	//这里可以设定只推送给这个sid的，为null则全部推送
            	if(sid==null) {
            		item.sendMessage(message);
            	}else if(item.sid.equals(sid)){
            		item.sendMessage(message);
            		break;
            	}
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
    
}

