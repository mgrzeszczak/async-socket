package pl.mg.asyncsocket.examples;

import pl.mg.asyncsocket.*;

class App {

    public static void main(String[] args) throws InterruptedException {
        StringMessageReader reader = new StringMessageReader();
        AsyncSocket.server(new Server(reader), 8080);
        AsyncSocket.client(new Client(reader), "localhost", 8080);
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            System.out.println("[MAIN THREAD]");
        }
        AsyncSocket.shutdown();
    }

}
