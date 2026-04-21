package network.protocol.ed2k;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

/**
 * Respuesta con lista de fuentes (OP_ANSWERSOURCES2 = 0x84).
 * Contiene el hash del archivo y la lista de clientes que lo poseen.
 */
public class SourceResponse extends Ed2kMessage {
    private final byte[] fileHash;
    private final List<SourceInfo> sources;

    public static class SourceInfo {
        public final long ip;
        public final int port;

        public SourceInfo(long ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public String toString() {
            return String.format("%d.%d.%d.%d:%d", 
                ip & 0xFF, (ip >> 8) & 0xFF, (ip >> 16) & 0xFF, (ip >> 24) & 0xFF, port);
        }
    }

    public SourceResponse(byte[] fileHash, List<SourceInfo> sources, byte opcode) {
        super(opcode);
        this.fileHash = fileHash;
        this.sources = sources;
    }

    @Override
    public void encode(ByteBuf out) {
        if (getOpcode() == (byte) 0x84) {
            out.writeByte(4); // Versión 4
            out.writeBytes(fileHash);
        } else {
            out.writeBytes(fileHash);
        }
        out.writeByte(sources.size());
        for (SourceInfo src : sources) {
            out.writeIntLE((int) src.ip);
            out.writeShortLE(src.port);
        }
    }

    public static SourceResponse decode(ByteBuf in, byte opcode) {
        byte[] hash = new byte[16];
        int version = 0;
        
        switch (opcode) {
            case Ed2kConstants.OP_ANSWERSOURCES2:
                version = in.readUnsignedByte();
                in.readBytes(hash);
                break;
            case Ed2kConstants.OP_FOUNDSOURCES_EMULE:
            case Ed2kConstants.OP_FOUNDSOURCES:
            default:
                in.readBytes(hash);
                break;
        }

        int count = in.readUnsignedByte();
        List<SourceInfo> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            long ip = in.readUnsignedIntLE();
            int port = in.readUnsignedShortLE();
            
            if (opcode == Ed2kConstants.OP_ANSWERSOURCES2) {
                if (version >= 2) in.skipBytes(6);
                if (version >= 3) in.skipBytes(16);
            }
            
            list.add(new SourceInfo(ip, port));
        }
        return new SourceResponse(hash, list, opcode);
    }

    public List<SourceInfo> getSources() {
        return sources;
    }
}
