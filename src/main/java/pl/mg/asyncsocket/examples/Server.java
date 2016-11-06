package pl.mg.asyncsocket.examples;

import pl.mg.asyncsocket.*;


class Server implements AsyncServer<StringMessage> {

    private final StringMessageReader stringMessageReader;
    private ServerInstance serverInstance;

    public Server(StringMessageReader reader){
        this.stringMessageReader = reader;
    }

    @Override
    public void onOpen(Connection connection) {
        System.out.println("[SERVER] New connection @"+connection.getHost()+":"+connection.getPort());
    }

    @Override
    public void onMessage(Connection connection, StringMessage message) {
        System.out.println("[SERVER] New message from "+connection.getHost()+":"+connection.getPort()+" :"+message
                .getContent());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connection.send(new StringMessage(String.valueOf(Integer.parseInt(message.getContent())+1)));
    }

    @Override
    public void onError(Connection connection, Exception e) {

    }

    @Override
    public void onClose(Connection connection) {
        System.out.println("[SERVER] Connection closing "+connection.getHost()+":"+connection.getPort());
    }


    @Override
    public void onServerOpen(ServerInstance serverInstance) {
        System.out.println("[SERVER] onServerOpen");
        this.serverInstance = serverInstance;
    }

    @Override
    public void onServerError(Exception e) {
        System.out.println("[SERVER] Server encountered a critical error:");
        System.out.println(e.getMessage());
    }

    @Override
    public void onServerClose() {
        System.out.println("[SERVER] Server closed");
    }

    @Override
    public StringMessage readMessage(ByteReader reader) throws InsufficientByteAmountException {
        return this.stringMessageReader.readMessage(reader);
    }
}
