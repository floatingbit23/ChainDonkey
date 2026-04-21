package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Estado de Identificación Segura (OP_SECIDENTSTATE = 0x87).
 * Se utiliza para intercambiar el estado y el reto (challenge).
 */
public class Ed2kSecureIdent extends Ed2kMessage {
    private final byte state;
    private final int challenge;

    public static final byte IS_SIGNATURENEEDED = 1;
    public static final byte IS_KEYANDSIGNEEDED = 2;

    public Ed2kSecureIdent(byte state, int challenge) {
        super(Ed2kConstants.OP_SECIDENTSTATE);
        this.state = state;
        this.challenge = challenge;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeByte(state);
        out.writeIntLE(challenge);
    }

    public static Ed2kSecureIdent decode(ByteBuf in) {
        if (in.readableBytes() < 5) return null;
        byte state = in.readByte();
        int challenge = in.readIntLE();
        return new Ed2kSecureIdent(state, challenge);
    }

    public byte getState() {
        return state;
    }

    public int getChallenge() {
        return challenge;
    }
}
