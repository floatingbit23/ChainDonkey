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
            case Ed2kConstants.TAG_TYPE_UINT16 -> out.writeShortLE(((Number) value).intValue());
            case Ed2kConstants.TAG_TYPE_UINT32 -> out.writeIntLE(((Number) value).intValue());
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
        byte rawType = in.readByte();
        byte type = (byte) (rawType & ~0x80);
        byte nameId = -1;
        String nameStr = null;

        if ((rawType & 0x80) != 0) {
            // Nombre de 1 byte (ID)
            nameId = in.readByte();
        } else {
            // Nombre String con longitud de 2 bytes
            int nameLength = in.readUnsignedShortLE();
            if (nameLength <= in.readableBytes()) {
                byte[] nameBytes = new byte[nameLength];
                in.readBytes(nameBytes);
                nameStr = new String(nameBytes, StandardCharsets.UTF_8);
                // Si el nombre es de 1 byte, lo guardamos como ID de todos modos
                if (nameLength == 1) nameId = nameBytes[0];
            } else {
                logger.warn("Longitud de nombre de etiqueta {} excede los bytes disponibles.", nameLength);
            }
        }

        try {
            Object value = switch (type) {
                case Ed2kConstants.TAG_TYPE_UINT8 -> (int)in.readUnsignedByte();
                case Ed2kConstants.TAG_TYPE_UINT32, 0x09 -> in.readUnsignedIntLE(); // 0x09 se usa como 32-bit en Sunrise
                case Ed2kConstants.TAG_TYPE_STRING, 0x17 -> {
                    if (in.readableBytes() < 2) yield null;
                    int strLen = in.readUnsignedShortLE();
                    if (strLen > 0 && strLen <= 1024 && strLen <= in.readableBytes()) {
                        byte[] strBytes = new byte[strLen];
                        in.readBytes(strBytes);
                        yield new String(strBytes, StandardCharsets.UTF_8);
                    } else {
                        // Si la longitud es absurda, es que estamos desincronizados
                        logger.warn("Longitud de string sospechosa ({}). Posible desincronización de tags.", strLen);
                        yield null;
                    }
                }
                case Ed2kConstants.TAG_TYPE_FLOAT -> in.readFloatLE();
                case Ed2kConstants.TAG_TYPE_HASH -> {
                    if (in.readableBytes() >= 16) {
                        byte[] hashBytes = new byte[16];
                        in.readBytes(hashBytes);
                        yield hashBytes;
                    } else {
                        yield null;
                    }
                }
                default -> {
                    logger.warn("Tipo de etiqueta desconocido: 0x{} (Raw: 0x{})", String.format("%02X", type), String.format("%02X", rawType));
                    yield null;
                }
            };
            return new Ed2kTag(type, nameId, value);
        } catch (Exception e) {
            logger.error("Error al decodificar etiqueta (Tipo: 0x{}, ID: 0x{}): {}", 
                String.format("%02X", type), String.format("%02X", nameId), e.getMessage());
            return null; // Devolvemos null para indicar que este tag falló
        }
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
