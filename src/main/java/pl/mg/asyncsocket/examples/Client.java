package pl.mg.asyncsocket.examples;

import pl.mg.asyncsocket.*;

class Client implements AsyncClient<StringMessage> {

    private final StringMessageReader reader;

    public Client(StringMessageReader reader){
        this.reader = reader;
    }

    @Override
    public void onMessage(Connection connection, StringMessage message) {
        System.out.println("[CLIENT] New message: "+message.getContent());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connection.send(new StringMessage(String.valueOf(Integer.parseInt(message.getContent())+1)));
    }

    @Override
    public void onClose(Connection connection) {
        System.out.println("[CLIENT] onClose "+connection.getHost()+" "+connection.getPort());
    }

    @Override
    public void onOpen(Connection connection) {
        System.out.println("[CLIENT] onOpen");
        connection.send(new StringMessage("1"));
    }

    @Override
    public void onError(Connection connection, Exception e) {
        System.out.println("[CLIENT] onError "+e.getMessage());
    }

    @Override
    public StringMessage readMessage(ByteReader reader) throws InsufficientByteAmountException {
        return this.reader.readMessage(reader);
    }
}
