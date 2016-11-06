package pl.mg.asyncsocket;

public final class AsyncSocket {

    private AsyncSocket(){
        throw new AssertionError("No instances.");
    }

    public static <T extends Message> void client(AsyncClient<T> client, String host, int port){
        BackgroundWorker.INSTANCE.connect(new ClientData<>(
                client,port,client,host
        ));
    }

    public static <T extends Message> void server(AsyncServer<T> server,int port){
        BackgroundWorker.INSTANCE.server(new ServerData<>(
                server,port,server
        ));
    }

    public static void shutdown(){
        BackgroundWorker.INSTANCE.shutdown();
    }

}
