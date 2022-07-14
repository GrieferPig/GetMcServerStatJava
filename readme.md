# GetMcServerStat

An implementation of Minecraft's [Server List Ping API](https://wiki.vg/Server_List_Ping) in pure Java.

# How to use

        GetMcServerStat gmss = new GetMcServerStat("ur.server.addr", (char)25565);
        System.out.println(gmss.getServerStat().description);
        System.out.println(gmss.ping());
        gmss.close();

Upon `GetMcServerStat` is created, a connection will establish to the server address for further actions.

After this, you can use `getServerStat()` to get server's info like title, max players, etc.
In fact, it will return an `MsgModel` object containing infos returned from server.

Then you may use `ping()` to calibrate the latency from server to client. ___YOU CAN ONLY USE `ping()` ONCE AFTER `getServerStat()` DUE TO API'S LIMITATIONS.___

Don't forget to close the connection using `close()` in the end!