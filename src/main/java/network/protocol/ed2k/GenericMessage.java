package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Mensaje genérico para manejar opcodes aún no implementados.
 * Útil para depuración y para evitar que el codec ignore paquetes desconocidos.
 */
public class GenericMessage extends Ed2kMessage {
    private final ByteBuf data;

    public GenericMessage(byte opcode, ByteBuf data) {
        super(opcode);
        this.data = data.retain();
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeBytes(data);
    }

    public ByteBuf getData() {
        return data;
    }

    @Override
    public String toString() {
        return "GenericMessage{opcode=0x" + String.format("%02X", getOpcode()) + ", size=" + data.readableBytes() + "}";
    }
}
