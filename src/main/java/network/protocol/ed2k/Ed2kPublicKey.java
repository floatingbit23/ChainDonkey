package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Mensaje para intercambiar la clave pública (OP_PUBLICKEY = 0x85).
 */
public class Ed2kPublicKey extends Ed2kMessage {
    private final byte[] publicKey;

    public Ed2kPublicKey(byte[] publicKey) {
        super(Ed2kConstants.OP_PUBLICKEY);
        this.publicKey = publicKey;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeByte(publicKey.length);
        out.writeBytes(publicKey);
    }

    public static Ed2kPublicKey decode(ByteBuf in) {
        if (in.readableBytes() < 1) return null;
        int len = in.readUnsignedByte();
        if (in.readableBytes() < len) return null;
        byte[] key = new byte[len];
        in.readBytes(key);
        return new Ed2kPublicKey(key);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
