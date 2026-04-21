package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Petición de fuentes para un archivo (OP_REQUESTSOURCES2 = 0x83).
 * Se utiliza en el intercambio de fuentes (Source Exchange) entre clientes.
 */
public class SourceRequest extends Ed2kMessage {
    private final byte[] fileHash;
    private final byte sxVersion;
    private final int options;
    private byte opcode = Ed2kConstants.OP_GETSOURCES_EXT; // Opcode dinámico

    public SourceRequest(byte[] fileHash) {
        this(fileHash, (byte) 4, 0);
    }

    public SourceRequest(byte[] fileHash, byte sxVersion, int options) {
        super(Ed2kConstants.OP_GETSOURCES_EXT);
        if (fileHash.length != 16) {
            throw new IllegalArgumentException("El hash del archivo debe tener 16 bytes");
        }
        this.fileHash = fileHash;
        this.sxVersion = sxVersion;
        this.options = options;
    }
    
    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    @Override
    public byte getOpcode() {
        return this.opcode;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeBytes(fileHash);
        if (opcode == Ed2kConstants.OP_GETSOURCES_EXT || opcode == (byte)0x83) {
            out.writeByte(sxVersion);
            out.writeShortLE(options);
        }
        // Para opcode 0x81 no se añade nada más, solo el Hash
    }

    public static SourceRequest decode(ByteBuf in) {
        byte[] hash = new byte[16];
        in.readBytes(hash);
        byte version = in.readableBytes() >= 1 ? in.readByte() : 4;
        int opts = in.readableBytes() >= 2 ? in.readUnsignedShortLE() : 0;
        return new SourceRequest(hash, version, opts);
    }

    public byte[] getFileHash() {
        return fileHash;
    }
}
