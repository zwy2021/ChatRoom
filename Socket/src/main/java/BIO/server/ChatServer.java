package BIO.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 监听客户端建立连接的请求
 * 并将该客户端发的信息转发到其他客户端
 */
public class ChatServer {
    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private HashMap<Integer, Writer> connectedClients;

    public ChatServer() {
        executorService= Executors.newFixedThreadPool(10);
        connectedClients = new HashMap<>();
    }

    //函数中的exception都在调用者里面处理
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            connectedClients.put(socket.getPort(), new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            System.out.println("客户端[" + socket.getPort() + "]已连接到服务器");
        }
    }

    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("客户端[" + socket.getPort() + "]已断开连接");
        }
    }

    //转发
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Integer integer : connectedClients.keySet()) {
            if (!integer.equals(socket.getPort())) {
                Writer writer = connectedClients.get(integer);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    //主逻辑，处理exception
    public void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口：[" + DEFAULT_PORT + "...");
            while (true) {
                Socket socket = serverSocket.accept();
                //创建ChatHandler线程
//                new Thread(new ChatHandler(this,socket)).start();
                //服务端改用线程池
                executorService.execute(new ChatHandler(this,socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public boolean readyToQuit(String msg) {
        //不是forwardMessage
        return QUIT.equals(msg);
    }

    public synchronized void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("已关闭ServeSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}