package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Representa el mensaje eD2K OP_SERVERMESSAGE (0x38).
 *
 * Este mensaje es enviado por un servidor a un cliente para transmitir texto.
 * Suele contener el MOTD (Message of the Day), advertencias o razones de desconexión.
 */
public class ServerMessage extends Ed2kMessage {

    private final String message;

    public ServerMessage(String message) {
        super(Ed2kConstants.OP_SERVERMESSAGE);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void encode(ByteBuf out) {
        byte[] bytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeShortLE(bytes.length);
        out.writeBytes(bytes);
    }

    public static ServerMessage decode(ByteBuf in) {
        int length = in.readUnsignedShortLE();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        String msg = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        return new ServerMessage(msg);
    }
}
