package my.xtream;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class Server {
    Xtream xtream;

    Server(Xtream Xtream) {
        xtream = Xtream;
    }

    public void run(int port) {
        Javalin app = Javalin.create().start(port);
        app.get("/*", this::process);
    }

    private void process(Context ctx) {
        String action = ctx.queryParam("action");
        String result=null;

        if ("/player_api.php".equals(ctx.path())) {
            result = xtream.apiCmd(action);
        }
        if (ctx.path().startsWith("/get.php")) {
            result = xtream.apiCmd("getM3U");
        }

        String qs="";
        if (ctx.queryString() != null) {
            qs="?"+ctx.queryString();
        };
        if ("/xmltv.php".equals(ctx.path()) && xtream.getEPG() != null) {
            System.err.println("Redirect -> "+xtream.getEPG());
            ctx.redirect(xtream.getEPG());
        } else if (result == null) {
            String redirect = xtream.getRedirectURL()+ctx.path()+qs;
            System.err.println("Redirect -> "+redirect);
            ctx.redirect(redirect);
        } else {
            System.err.println("Cache -> "+ctx.path()+qs);
            ctx.json(result);
        }
    }
}