package pl.mg.asyncsocket;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

enum BackgroundWorker {

    /**************************************************
     *
     * Singleton instance
     *
     **************************************************/
    INSTANCE;

    /**************************************************
     *
     * Constants
     *
     **************************************************/
    private final static boolean DEBUG = false;
    private final static int BUFFER_SIZE = 2048;
    private final static int SO_TIMEOUT = 15; // millis
    private final static Connection NULL_CONNECTION = new Connection(){

        @Override
        public void send(Message message) {

        }

        @Override
        public void close() {

        }

        @Override
        public String getHost() {
            return "";
        }

        @Override
        public int getPort() {
            return 0;
        }
    };

    private final Task POISON_PILL = new Task(){

        @Override
        protected void execute() {

        }

        @Override
        protected boolean compare(Object object) {
            return false;
        }
    };

    //private static BackgroundWorker instance;
    /*static BackgroundWorker getInstance(){
        //if (instance==null) instance = new BackgroundWorker();
        //return instance;
        return SingletonWrapper.instance;
    }

    private static class SingletonWrapper {
        private static final BackgroundWorker instance = new BackgroundWorker();
    }/*

    /**************************************************
     *
     * Private variables
     *
     **************************************************/
    private Map<Socket,SocketData> socketDataMap;
    private Map<ServerSocket, ServerSocketData> serverSocketDataMap;

    /**
     * Task queue
     */
    private LinkedBlockingQueue<Task> queue;
    /**
     * Thread pool for events
     */
    private ThreadPool threadPool;
    /**
     * This worker's thread
     */
    private Thread thread;

    /**************************************************
     *
     * Constructor
     *
     **************************************************/
    BackgroundWorker(){
        this.queue = new LinkedBlockingQueue<>();
        this.threadPool = new ThreadPool();

        this.socketDataMap = new ConcurrentHashMap<>();
        this.serverSocketDataMap = new ConcurrentHashMap<>();

        start();
    }

    /**************************************************
     *
     * Public API
     *
     **************************************************/
    void connect(ClientData<?> data){
        scheduleTask(new ConnectSocketTask(data));
    }

    void server(ServerData<?> data){
        scheduleTask(new StartServerTask(data));
    }

    void shutdown(){
        scheduleTask(new ExitPlatformTask());
        try {
            this.thread.join();
            this.threadPool.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**************************************************
     *
     * Private methods
     *
     **************************************************/

    private synchronized void scheduleTask(Task task){
        queue.offer(task);
    }

    private void run(){
        while(true){
            try {
                Task task = queue.take();
                if (DEBUG) debug(task);
                if (task==POISON_PILL) {
                    if (DEBUG) System.out.println("POISON PILL");
                    return;
                }
                task.execute();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unused")
    private void debug(Task task){
        System.out.println(task.getClass().getSimpleName());
    }

    private void start(){
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BackgroundWorker.this.run();
            }
        });
        this.thread.start();
    }

    private Connection createConnection(final Socket socket){
        return new Connection() {

            private String host = socket.getInetAddress().getHostName();
            private int port = socket.getPort();

            @Override
            public void send(Message message) {
                scheduleTask(new SyncScheduleTask(new WriteSocketTask(socket,message.getBytes())));
            }

            @Override
            public void close() {
                scheduleTask(new SyncScheduleTask(new CloseSocketTask(socket)));
            }

            @Override
            public String getHost() {
                return host;
            }

            @Override
            public int getPort() {
                return port;
            }
        };
    }

    private ServerInstance createServerInstance(final ServerSocket serverSocket){
        return new ServerInstance() {
            @Override
            public void close() {
                scheduleTask(new SyncScheduleTask(new CloseServerTask(serverSocket)));
            }
        };
    }

    /**************************************************
     *
     * Task definition
     *
     **************************************************/
    private abstract class Task {
        abstract protected void execute();
        abstract protected boolean compare(Object object);
    }

    private abstract class SocketTask extends Task {

        protected final Socket socket;

        SocketTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        protected boolean compare(Object object) {
            return this.socket == object;
        }
    }

    private abstract class ServerTask extends Task {

        protected final ServerSocket serverSocket;

        ServerTask(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        protected boolean compare(Object object) {
            return this.serverSocket == object;
        }
    }

    private final class ExitPlatformTask extends Task {

        @Override
        protected void execute() {
            //socketDataMap.keySet().forEach(s->scheduleTask(new CloseSocketTask(s)));
            //serverSocketDataMap.keySet().forEach(s->scheduleTask(new CloseServerTask(s)));
            for (Socket s : socketDataMap.keySet()){
                scheduleTask(new CloseSocketTask(s));
            }
            for (ServerSocket s : serverSocketDataMap.keySet()){
                scheduleTask(new CloseServerTask(s));
            }
            scheduleTask(POISON_PILL);
        }

        @Override
        protected boolean compare(Object object) {
            return false;
        }
    }

    private final class SyncScheduleTask extends Task {

        private final Task task;

        SyncScheduleTask(Task task) {
            this.task = task;
        }

        @Override
        protected void execute() {
            scheduleTask(task);
        }

        @Override
        protected boolean compare(Object object) {
            return false;
        }
    }


    /**************************************************
     *
     * SocketTasks definition
     *
     **************************************************/
    private final class ConnectSocketTask extends Task {

        private final ClientData<?> clientData;

        ConnectSocketTask(ClientData<?> clientData) {
            this.clientData = clientData;
        }

        @Override
        protected void execute() {
            try {
                Socket socket = new Socket(clientData.getHost(), clientData.getPort());
                socket.setSoTimeout(SO_TIMEOUT);

                final Connection connection = createConnection(socket);

                socketDataMap.put(socket,new SocketData(clientData,connection));

                //threadPool.execute(()-> clientData.getEventHandler().onOpen(connection));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        clientData.getEventHandler().onOpen(connection);
                    }
                });
                scheduleTask(new ReadSocketTask(socket));
            } catch (final Exception e){
                // Caught when an error occured
                // Schedule error and close event
                //threadPool.execute(()-> clientData.getEventHandler().onError(NULL_CONNECTION,e));
                //threadPool.execute(()-> clientData.getEventHandler().onClose(NULL_CONNECTION));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        clientData.getEventHandler().onError(NULL_CONNECTION,e);
                    }
                });
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        clientData.getEventHandler().onClose(NULL_CONNECTION);
                    }
                });
            }
        }

        @Override
        protected boolean compare(Object object) {
            return false;
        }
    }

    private final class ReadSocketTask extends SocketTask {

        ReadSocketTask(Socket socket) {
            super(socket);
        }

        @Override
        protected void execute() {
            final SocketData data = socketDataMap.get(socket);
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                int read = socket.getInputStream().read(buffer,0,BUFFER_SIZE);
                if (read==-1) {
                    scheduleTask(new CloseSocketTask(socket));
                    return;
                }
                //ByteBuffer byteBuffer = byteBufferMap.get(socket);
                ByteBuffer byteBuffer = data.byteBuffer;
                byteBuffer.push(buffer,0,read);
            } catch (SocketTimeoutException e){
                scheduleTask(new ReadSocketTask(socket));
                return;
            }
            catch (final Exception e){
                // Caught when an error occured
                // Start error event
                //threadPool.execute(()->connectionDataMap.get(socket).getEventHandler().onError(e));

                //threadPool.execute(()->data.clientData.getEventHandler().onError(data.connection,e));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        data.clientData.getEventHandler().onError(data.connection,e);
                    }
                });
                // Clear all tasks by this socket
                //queue.removeIf(task->task.compare(socket));
                for (Iterator<Task> it = queue.iterator();it.hasNext();){
                    Task task = it.next();
                    if (task.compare(socket)) it.remove();
                }

                // Schedule close task
                scheduleTask(new CloseSocketTask(socket));
                return;
            }
            scheduleTask(new ReadProtocolMessageSocketTask(socket));
            scheduleTask(new ReadSocketTask(socket));
        }
    }

    private final class ReadProtocolMessageSocketTask extends SocketTask {

        ReadProtocolMessageSocketTask(java.net.Socket socket) {
            super(socket);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void execute() {
            SocketData socketData = socketDataMap.get(socket);
            final ClientData data = socketData.clientData;
            final Connection connection = socketData.connection;
            ByteBuffer byteBuffer = socketData.byteBuffer;
            byteBuffer.save();
            try {
                final Message message = data.getMessageReader().readMessage(new ByteReaderImpl(byteBuffer));

                //threadPool.execute(()-> data.getEventHandler().onMessage(connection,message));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        data.getEventHandler().onMessage(connection,message);
                    }
                });
            } catch (InsufficientByteAmountException e){
                byteBuffer.restore();
            }
        }
    }

    private final class WriteSocketTask extends SocketTask {

        private final byte[] message;

        WriteSocketTask(Socket socket, byte[] message) {
            super(socket);
            this.message = message;
        }

        @Override
        protected void execute() {
            if (!socketDataMap.containsKey(socket)) return;
            final SocketData data = socketDataMap.get(socket);
            try {
                socket.getOutputStream().write(message);
            } catch (final Exception e){
                // Caught when an error occured
                // Start error event
                //threadPool.execute(()->data.clientData.getEventHandler().onError(data.connection,e));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        data.clientData.getEventHandler().onError(data.connection,e);
                    }
                });
                //threadPool.execute(()->connectionDataMap.get(socket).getEventHandler().onError(e));
                // Clear all tasks by this socket
                //queue.removeIf(task->task.compare(socket));
                for (Iterator<Task> it = queue.iterator();it.hasNext();){
                    Task task = it.next();
                    if (task.compare(socket)) it.remove();
                }
                // Schedule close task
                scheduleTask(new CloseSocketTask(socket));
            }
        }
    }

    private final class CloseSocketTask extends SocketTask {

        CloseSocketTask(Socket socket) {
            super(socket);
        }

        @Override
        protected void execute() {
            if (!socketDataMap.containsKey(socket)) return;
            try {
                socket.close();
            } catch (Exception e) {
                // Should not happen, if it does it is already closed
                // Ignore
            }
            //queue.removeIf(task->task.compare(socket));
            for (Iterator<Task> it = queue.iterator();it.hasNext();){
                Task task = it.next();
                if (task.compare(socket)) it.remove();
            }

            final SocketData data = socketDataMap.get(socket);
            socketDataMap.remove(socket);
            if (data.server!=null && serverSocketDataMap.containsKey(data.server))
                serverSocketDataMap.get(data.server).clients.remove(socket);

            //threadPool.execute(()->data.clientData.getEventHandler().onClose(data.connection));
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    data.clientData.getEventHandler().onClose(data.connection);
                }
            });
        }
    }


    /**************************************************
     *
     * ServerTasks definition
     *
     **************************************************/
    private final class StartServerTask extends Task {

        private final ServerData<?> serverData;

        StartServerTask(ServerData<?> serverData) {
            this.serverData = serverData;
        }

        @Override
        protected void execute() {
            try {
                ServerSocket serverSocket = new ServerSocket(serverData.getPort());
                serverSocket.setSoTimeout(SO_TIMEOUT);

                final ServerSocketData data = new ServerSocketData(serverData, createServerInstance(serverSocket));
                serverSocketDataMap.put(serverSocket,data);

                //threadPool.execute(()->serverData.getServerEventHandler().onServerOpen());
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        serverData.getServerEventHandler().onServerOpen(data.serverInstance);
                    }
                });
                scheduleTask(new AcceptClientServerTask(serverSocket));
            } catch (final Exception e){
                //threadPool.execute(()->serverData.getServerEventHandler().onServerError(e));
                //threadPool.execute(()->serverData.getServerEventHandler().onServerClose());
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        serverData.getServerEventHandler().onServerError(e);
                    }
                });
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        serverData.getServerEventHandler().onServerClose();
                    }
                });
            }
        }

        @Override
        protected boolean compare(Object object) {
            return false;
        }
    }

    private final class CloseServerTask extends ServerTask {

        CloseServerTask(ServerSocket serverSocket) {
            super(serverSocket);
        }

        @Override
        protected void execute() {
            if (!serverSocketDataMap.containsKey(serverSocket)) return;
            final ServerSocketData data = serverSocketDataMap.get(serverSocket);
            try {
                serverSocket.close();
            } catch (Exception e){
                // Not expected
            }
            //data.clients.forEach(s->queue.removeIf(task->task.compare(s)));
            //data.clients.forEach(s->scheduleTask(new CloseSocketTask(s)));

            for (Socket s : data.clients){
                for (Iterator<Task> it = queue.iterator();it.hasNext();){
                    Task task = it.next();
                    if (task.compare(s)) it.remove();
                }
                scheduleTask(new CloseSocketTask(s));
            }

            serverSocketDataMap.remove(serverSocket);
            //queue.removeIf(task->task.compare(serverSocket));
            for (Iterator<Task> it = queue.iterator();it.hasNext();){
                Task task = it.next();
                if (task.compare(serverSocket)) it.remove();
            }
            //threadPool.execute(()->data.serverData.getServerEventHandler().onServerClose());
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    data.serverData.getServerEventHandler().onServerClose();
                }
            });
        }
    }

    private final class AcceptClientServerTask extends ServerTask {

        AcceptClientServerTask(ServerSocket serverSocket) {
            super(serverSocket);
        }

        @Override
        @SuppressWarnings("all")
        protected void execute() {
            final ServerSocketData serverSocketData = serverSocketDataMap.get(serverSocket);
            try {
                Socket client = serverSocket.accept();
                client.setSoTimeout(SO_TIMEOUT);

                serverSocketData.clients.add(client);

                final Connection connection = createConnection(client);

                final ClientData<?> clientData = new ClientData(serverSocketData.serverData.getMessageReader()
                        ,client.getPort(),serverSocketData.serverData.getServerEventHandler(),client.getInetAddress()
                        .getHostName());

                socketDataMap.put(client,new SocketData(serverSocket,clientData, connection));

                //threadPool.execute(()-> clientData.getEventHandler().onOpen(connection));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        clientData.getEventHandler().onOpen(connection);
                    }
                });
                scheduleTask(new ReadSocketTask(client));

            } catch (SocketTimeoutException e){

            } catch (final Exception e) {
                //threadPool.execute(()->serverSocketData.serverData.getServerEventHandler().onServerError(e));
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        serverSocketData.serverData.getServerEventHandler().onServerError(e);
                    }
                });
                //queue.removeIf(task->task.compare(serverSocket));
                for (Iterator<Task> it = queue.iterator();it.hasNext();){
                    Task task = it.next();
                    if (task.compare(serverSocket)) it.remove();
                }
                scheduleTask(new CloseServerTask(serverSocket));
                return;
            }
            scheduleTask(new AcceptClientServerTask(serverSocket));
        }
    }

    /**************************************************
     *
     * Private data structures
     *
     **************************************************/

    private final class SocketData {

        private final ClientData<?> clientData;
        private final ByteBuffer byteBuffer;
        private final Connection connection;
        private final ServerSocket server;

        SocketData(ClientData clientData, Connection connection) {
            this.clientData = clientData;
            this.connection = connection;
            this.byteBuffer = new ByteBuffer();
            this.server = null;
        }

        SocketData(ServerSocket server,ClientData<?> clientData,Connection connection){
            this.clientData = clientData;
            this.connection = connection;
            this.byteBuffer = new ByteBuffer();
            this.server = server;
        }

    }

    private final class ServerSocketData {

        private final ServerData<?> serverData;
        @SuppressWarnings("all")
        private final Set<Socket> clients;
        private final ServerInstance serverInstance;

        ServerSocketData(ServerData<?> serverData, ServerInstance serverInstance) {
            this.serverData = serverData;
            this.clients = new HashSet<>();
            this.serverInstance = serverInstance;
        }

    }
}
