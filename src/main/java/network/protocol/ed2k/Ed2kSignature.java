package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Mensaje para enviar la firma RSA (OP_SIGNATURE = 0x86).
 */
public class Ed2kSignature extends Ed2kMessage {
    private final byte[] signature;

    public Ed2kSignature(byte[] signature) {
        super(Ed2kConstants.OP_SIGNATURE);
        this.signature = signature;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeByte(signature.length);
        out.writeBytes(signature);
    }

    public static Ed2kSignature decode(ByteBuf in) {
        if (in.readableBytes() < 1) return null;
        int len = in.readUnsignedByte();
        if (in.readableBytes() < len) return null;
        byte[] sig = new byte[len];
        in.readBytes(sig);
        return new Ed2kSignature(sig);
    }

    public byte[] getSignature() {
        return signature;
    }
}
