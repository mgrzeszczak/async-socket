package pl.mg.asyncsocket;

final class ServerData<T extends Message> extends InstanceData<T>{

    private final ServerEventHandler<T> serverEventHandler;

    ServerData(MessageReader<T> messageReader, int port, ServerEventHandler<T> serverEventHandler) {
        super(messageReader, port);
        this.serverEventHandler = serverEventHandler;
    }

    ServerEventHandler<T> getServerEventHandler() {
        return serverEventHandler;
    }

}
