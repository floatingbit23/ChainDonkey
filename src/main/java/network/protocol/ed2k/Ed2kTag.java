package network.protocol.ed2k;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

/**
 * Representa una etiqueta (tag) del protocolo eD2K.
 * Las etiquetas se utilizan para añadir metadatos a los mensajes.
 */
public class Ed2kTag {

    private static final Logger logger = LoggerFactory.getLogger(Ed2kTag.class);

    private final byte type;
    private final byte name; // ID de la etiqueta (nombre codificado como un byte en la mayoría de casos)
    private final Object value;

    public Ed2kTag(byte type, byte name, Object value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public byte getType() {
        return type;
    }

    public byte getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Escribe esta etiqueta en un ByteBuf de Netty.
     *
     * @param out el búfer al que escribir
     */
    public void writeToBuffer(ByteBuf out) {
        // Marcamos el tipo con 0x80 para indicar que el nombre es un solo byte (ID)
        out.writeByte(type | (byte)0x80);
        out.writeByte(name);

        switch (type & ~0x80) {
            case Ed2kConstants.TAG_TYPE_UINT8 -> out.writeByte(((Number) value).intValue());
            case Ed2kConstants.TAG_TYPE_UINT16, Ed2kConstants.TAG_TYPE_UINT32 -> out.writeIntLE(((Number) value).intValue());
            case Ed2kConstants.TAG_TYPE_STRING -> {
                String s = (String) value;
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                out.writeShortLE(bytes.length);
                out.writeBytes(bytes);
            }
            case Ed2kConstants.TAG_TYPE_FLOAT -> out.writeFloatLE(((Number) value).floatValue());
            case Ed2kConstants.TAG_TYPE_HASH -> out.writeBytes((byte[]) value);
            default -> logger.warn("Tipo de etiqueta desconocido para escritura: {}", type);
        }
    }

    public static Ed2kTag readFromBuffer(ByteBuf in) {
        if (in.readableBytes() < 1) return null;
        byte rawType = in.readByte();
        byte type = (byte) (rawType & ~0x80);
        byte nameId = -1;

        // Extraer ID o Nombre
        if ((rawType & 0x80) != 0) {
            if (in.readableBytes() < 1) return null;
            nameId = in.readByte();
        } else {
            if (in.readableBytes() < 2) return null;
            int nameLength = in.readUnsignedShortLE();
            if (nameLength <= in.readableBytes()) {
                in.skipBytes(nameLength); // Saltamos strings largos de nombres
            } else {
                return null; // Buffer corrupto
            }
        }

        Object value;
        
        // MAGIA: Strings Cortos Optimizados de eMule (Tipos 0x11 a 0x1F)
        if (type >= 0x11 && type <= 0x1F) {
            int strLen = type - 0x10;
            if (in.readableBytes() >= strLen) {
                byte[] strBytes = new byte[strLen];
                in.readBytes(strBytes);
                value = new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
            } else return null;
        } else {
            // Tipos Estándar
            switch (type) {
                case 0x02 -> { // String Normal
                    if (in.readableBytes() >= 2) {
                        int strLen = in.readUnsignedShortLE();
                        if (in.readableBytes() >= strLen) {
                            byte[] strBytes = new byte[strLen];
                            in.readBytes(strBytes);
                            value = new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
                        } else return null;
                    } else return null;
                }
                case 0x03, 0x09 -> { // Integers (Sunrise usa 0x09 como UInt32)
                    if (in.readableBytes() >= 4) value = in.readUnsignedIntLE();
                    else return null;
                }
                case 0x08 -> { // Byte
                    if (in.readableBytes() >= 1) value = (int)in.readUnsignedByte();
                    else return null;
                }
                default -> { return null; } // Tipo desconocido -> Abortamos tag para no desincronizar
            }
        }
        return new Ed2kTag(type, nameId, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ed2kTag ed2kTag = (Ed2kTag) o;
        return type == ed2kTag.type && name == ed2kTag.name && Objects.equals(value, ed2kTag.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, value);
    }
}
