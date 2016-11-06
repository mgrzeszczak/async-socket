package pl.mg.asyncsocket.examples;

import pl.mg.asyncsocket.Message;
import java.nio.ByteBuffer;

class StringMessage implements Message {

    private final static int INTEGER_SIZE = 4;

    private String content;

    public StringMessage(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = new byte[INTEGER_SIZE+content.length()];
        byte[] lengthBytes = ByteBuffer.allocate(INTEGER_SIZE).putInt(content.length()).array();
        System.arraycopy(lengthBytes,0,bytes,0,INTEGER_SIZE);
        System.arraycopy(content.getBytes(),0,bytes,INTEGER_SIZE,content.length());
        return bytes;
    }

}
