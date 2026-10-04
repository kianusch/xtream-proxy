package my.xtream;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class Server {
    Xtream xtream;
    String upstream;

    Server(Xtream xtream) {
        this.xtream = xtream;
    }

    public void run(int port) {
        Javalin app = Javalin.create().start(port);
        app.head("/*", this::processHead);
        app.get("/*", this::processGet);
    }

    private void processHead(Context ctx) {
        if ("/generate_204".equals(ctx.path())) {
            System.err.println("CONNECTIVITY CHECK: "+ctx.ip()+" -> " + ctx.path());
            ctx.status(204);
        }
    }

    private void processGet(Context ctx) {
        //byte[] inboundBody = ctx.bodyAsBytes();
        //String bodyStr = new String(inboundBody, java.nio.charset.StandardCharsets.UTF_8);
        //System.err.println("INBOUND BODY:");
        //System.err.println(bodyStr);

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
        // System.err.println("INBOUND -> "+ctx.path()+qs);
        if ("/xmltv.php".equals(ctx.path()) && xtream.getEPG() != null) {
            System.err.println("Redirect: "+ctx.ip()+" -> "+xtream.getEPG());
            ctx.redirect(xtream.getEPG());
        } else if (result == null){
            //System.err.println("IN: <"+ctx.path()+">|<"+ctx.queryString()+">");
            if (!"/".equals(ctx.path())) {
                String redirect = xtream.getRedirectURL() + ctx.path() + qs;
                System.err.println("Redirect: "+ctx.ip()+" -> "+redirect);
                ctx.redirect(redirect);
            } else {
                //System.err.println("IN: <"+ctx.path()+">|<"+ctx.queryString()+">");
                //System.err.println("HEADER: "+ctx.headerMap());
                //System.err.println("Return: "+ctx.ip()+" -> 200");
                ctx.status(403);
            }
        } else {
            System.err.println("Cache: "+ctx.ip()+ "-> "+ctx.path()+qs);
            ctx.json(result);
        }
    }



}