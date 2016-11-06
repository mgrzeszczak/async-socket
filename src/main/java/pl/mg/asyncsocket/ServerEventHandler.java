package pl.mg.asyncsocket;

interface ServerEventHandler<T extends Message> extends EventHandler<T>{

    void onServerOpen(ServerInstance serverInstance);
    void onServerError(Exception e);
    void onServerClose();

}
