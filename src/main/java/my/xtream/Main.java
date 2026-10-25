package my.xtream;

import java.io.IOException;

public class Main {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String[] args) throws IOException {
        String cfgPath = args.length < 1 ? "./xtream.cfg" : args[0];
        Server server = new Server(cfgPath);
        server.run();
    }
}
