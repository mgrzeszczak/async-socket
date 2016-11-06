package pl.mg.asyncsocket.examples;

import pl.mg.asyncsocket.ByteReader;
import pl.mg.asyncsocket.InsufficientByteAmountException;
import pl.mg.asyncsocket.MessageReader;
import java.nio.ByteBuffer;

class StringMessageReader implements MessageReader<StringMessage> {

    private final static int INTEGER_SIZE = 4;

    @Override
    public StringMessage readMessage(ByteReader reader) throws InsufficientByteAmountException {
        byte[] buffer = new byte[INTEGER_SIZE];
        reader.read(buffer,0,INTEGER_SIZE);
        int length = ByteBuffer.wrap(buffer).getInt();
        buffer = new byte[length];
        reader.read(buffer,0,length);
        return new StringMessage(new String(buffer));
    }
}
