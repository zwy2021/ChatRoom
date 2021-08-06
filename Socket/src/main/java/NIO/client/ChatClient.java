package NIO.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 和服务器建立连接，
 * 并将从服务接受的信息打印出来
 */
public class ChatClient {
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER=1024;
    private String host;
    private int port;
    private SocketChannel client;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Selector selector;
    private Charset charset=Charset.forName("UTF-8");

    public ChatClient() {
        this(DEFAULT_SERVER_PORT);


    }

    public ChatClient(int port) {
        this.port = port;
        host=DEFAULT_SERVER_HOST;
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close(Closeable closeable){
        if(closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void start() {
        try {
            client=SocketChannel.open();
            client.configureBlocking(false);
            selector=Selector.open();
            client.register(selector, SelectionKey.OP_CONNECT);
            client.connect(new InetSocketAddress(host,port));
            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    handle(key);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch(ClosedSelectorException e){
            //用户正常退出
        }finally {
            //selector关两次没事
            close(selector);
        }

    }

    private void handle(SelectionKey key) throws IOException {
        //连接就绪事件
        if(key.isConnectable()){
            SocketChannel client= (SocketChannel) key.channel();
            if(client.isConnectionPending()){
                //如果连接成功，就绪,正式建立连接
                client.finishConnect();
                //处理用户输入
                new Thread(new UserInputHandler(this)).start();
            }
            client.register(selector,SelectionKey.OP_READ);
        }
        //read转发服务器消息
        else if(key.isReadable()){
            SocketChannel client= (SocketChannel) key.channel();
            String msg=receive(client);
            if(msg.isEmpty()){
                //服务器异常
                close(selector);
            }else{
                System.out.println(msg);
            }
        }
    }

    public void send(String msg) throws IOException {
        if(msg.isEmpty()){
            return;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(msg));
        wBuffer.flip();
        while(wBuffer.hasRemaining()){
            client.write(wBuffer);
        }
        //检查用户是否准备退出
        if(readyToQuit(msg)){
            close(selector);
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while(client.read(rBuffer)>0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient(7777);
        chatClient.start();
    }
}