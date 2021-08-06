package NIO.server;

import BIO.server.ChatHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class ChatServer {
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER=1024;

    private ServerSocketChannel server;
    private Selector selector;
    //从client读取
    private ByteBuffer rBuffer=ByteBuffer.allocate(BUFFER);
    //转发
    private ByteBuffer wBuffer=ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");
    //存储用户自定义端口，和有参构造函数配合
    private int port;

    //主逻辑，处理exception
    public void start() {
        try {
            server=ServerSocketChannel.open();
            //关闭阻塞
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));

            selector=Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口："+port+"...");
            while (true) {
                //阻塞式，注册的监听事件发生才返回
                selector.select();
                //返回处触发的事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    //处理被触发的事件
                    handles(key);
                }
                //清除一下
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关闭
            close(selector);
        }
    }

    public void handles(SelectionKey key) throws IOException {
        //Accept事件-和客户端建立连接
        if(key.isAcceptable()){
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client=server.accept();
            client.configureBlocking(false);
            client.register(selector,SelectionKey.OP_READ);
            System.out.println(getClientName(client)+"已连接");
        }
        //Read事件- 客户端发送了信息
        else if(key.isReadable()){
            SocketChannel client = (SocketChannel) key.channel();
            String fwdMsg=receive(client);
            if(fwdMsg.isEmpty()){
                //客户端异常，selector停止监听
                key.cancel();
                selector.wakeup();
            }else{
                forwardMessage(client,fwdMsg);
                System.out.println(getClientName(client)+":"+fwdMsg);
                if(readyToQuit(fwdMsg)){
                    key.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client)+"已断开连接");
                }
            }
        }
    }

    private void forwardMessage(SocketChannel client, String fwdMsg) throws IOException {
        for (SelectionKey key : selector.keys()) {
            Channel connectedClient=key.channel();
            if(connectedClient instanceof ServerSocketChannel){
                continue;
            }
            //channel没被关闭，监视未被关闭
            if(key.isValid()&&!client.equals(connectedClient)){
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(client)+":"+fwdMsg));
                wBuffer.flip();
                while(wBuffer.hasRemaining()){
                    ((SocketChannel)connectedClient).write(wBuffer);
                }
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer)>0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private String getClientName(SocketChannel client){
        return "客户端["+client.socket().getPort()+"]";
    }
    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    public boolean readyToQuit(String msg) {
        //不是forwardMessage
        return QUIT.equals(msg);
    }

    public synchronized void close(Closeable closeable) {
        if(closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }
}