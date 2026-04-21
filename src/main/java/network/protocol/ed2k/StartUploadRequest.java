package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Petición de inicio de subida (OP_STARTUPLOADREQ = 0x47).
 * Estructura de 16 bytes.
 */
public class StartUploadRequest extends Ed2kMessage {
    private final byte[] fileHash;

    public StartUploadRequest(byte[] fileHash) {
        super(Ed2kConstants.OP_STARTUPLOADREQ);
        this.fileHash = fileHash;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeBytes(fileHash);
    }

    public static StartUploadRequest decode(ByteBuf in) {
        byte[] fHash = new byte[16];
        in.readBytes(fHash);
        return new StartUploadRequest(fHash);
    }

    public byte[] getFileHash() {
        return fileHash;
    }
}
