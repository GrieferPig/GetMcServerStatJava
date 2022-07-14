import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

public class GetMcServerStat {
    String addr;
    char port;
    Connection c;
    public GetMcServerStat(String addr, char port) throws IOException {
        this.addr = addr;
        this.port = port;
        this.c = new Connection(addr, port);
        this.c.connect();
    }

    public MsgModel getServerStat() throws IOException{
        PacketFactory.send(c.os, 0, Encoder.genHoofshake(addr, port));
        PacketFactory.sendRaw(c.os, new byte[]{0x01, 0x00});
        return new Gson().fromJson(PacketFactory.get(c.is, 0), MsgModel.class);
    }

    public int ping() throws IOException {
        PacketFactory.send(c.os, 1, new byte[8]);
        long start =  System.currentTimeMillis();
        c.is.read();
        long end =  System.currentTimeMillis();
        return (int) (end-start)/2;
    }

    public void close() throws IOException {
        c.rageQuit();
    }
}

class VarType {
    // adapted from https://wiki.vg/VarInt_And_VarLong

    public static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.read();

            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    public static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.write(paramInt);
                return;
            }

            out.write(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public static byte[] writeVarInt(int paramInt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                baos.write(paramInt);
                return baos.toByteArray();
            }
            baos.write(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public static byte[] toUShort(char value) {
        byte[] b = new byte[2];
        b[0] = (byte) ((value & 0xFF00) >> 8);
        b[1] = (byte) (value & 0xFF);
        return b;
    }

    public static byte[] writeString(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] str_utf8 = str.getBytes(StandardCharsets.UTF_8);
        baos.write(writeVarInt(str_utf8.length));
        baos.write(str_utf8);
        return baos.toByteArray();
    }
}

class PacketFactory {
    public static void send(DataOutputStream dos, int id, byte[] data) throws IOException {
        byte[] _idVartified = VarType.writeVarInt(id); // does this word "vartified" even exist
        int _dataLength = _idVartified.length + data.length;
        VarType.writeVarInt(dos, _dataLength);
        VarType.writeVarInt(dos, id);
        dos.write(data);
    }

    public static void sendRaw(DataOutputStream dos, byte[] data) throws IOException {
        dos.write(data);
    }

    public static String get(DataInputStream in, int expectId) throws IllegalArgumentException, IOException {
        VarType.readVarInt(in); // cya packet size
        int pId = VarType.readVarInt(in);
        if (pId != expectId) {
            throw new IllegalArgumentException("Packet ID does not match. Expecting " + pId + ", received " + expectId);
        }
        int dSize = VarType.readVarInt(in);
        int i;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(dSize);
        int c;
        for (c = 0; c != dSize; c++) {
            i = in.read();
            baos.write(i);
        }
        return baos.toString();
    }
}

class Encoder {
    public static byte[] genHoofshake(String serverAddr, char serverPort) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(47); // protocol version, default to 1.8.5's version

        baos.write(VarType.writeString(serverAddr)); // serverAddr, wrapped using MC's format
        baos.write(VarType.toUShort(serverPort)); //serverPort, in ushort

        baos.write(1); // next state

        return baos.toByteArray();
    }
}

class Connection {
    private final String addr;
    private final char port;

    public DataOutputStream os;
    public DataInputStream is;

    private Socket socket;

    public Connection(String addr, char port) {
        this.addr = addr;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(this.addr, this.port);
        this.os = new DataOutputStream(socket.getOutputStream());
        this.is = new DataInputStream(socket.getInputStream());
    }

    public void rageQuit() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
    }
}

class MsgModel {
    Version version;

    class Version {
        String name;
        int protocol;
    }

    Players players;

    class Players {
        int max;
        int online;
    }

    String description;
    String favicon;
}