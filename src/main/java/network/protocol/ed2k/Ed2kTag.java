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
        out.writeByte(type);
        out.writeShortLE(1); // Por ahora siempre usamos nombres de 1 byte (ID de etiqueta)
        out.writeByte(name);

        switch (type) {
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
        byte type = in.readByte();
        int nameLength = in.readUnsignedShortLE();
        byte nameId = -1;

        if (nameLength == 1 && in.readableBytes() >= 1) {
            nameId = in.readByte();
        } else {
            logger.info("Omitiendo etiqueta eD2K con nombre de longitud no estándar: {} bytes", nameLength);
            if (nameLength > 0 && nameLength <= in.readableBytes()) {
                in.skipBytes(nameLength);
            } else {
                logger.warn("Longitud de etiqueta {} excede los bytes disponibles.", nameLength);
            }
        }

        try {
            Object value = switch (type) {
                case Ed2kConstants.TAG_TYPE_UINT8 -> in.readUnsignedByte();
                case Ed2kConstants.TAG_TYPE_UINT16 -> in.readUnsignedShortLE();
                case Ed2kConstants.TAG_TYPE_UINT32 -> in.readUnsignedIntLE();
                case Ed2kConstants.TAG_TYPE_STRING -> {
                    int strLen = in.readUnsignedShortLE();
                    if (strLen <= in.readableBytes()) {
                        byte[] strBytes = new byte[strLen];
                        in.readBytes(strBytes);
                        yield new String(strBytes, StandardCharsets.UTF_8);
                    } else {
                        logger.warn("Longitud de string {} excede los bytes disponibles.", strLen);
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
                default -> null;
            };
            return new Ed2kTag(type, nameId, value);
        } catch (Exception e) {
            logger.error("Error al decodificar el valor de la etiqueta tipo 0x{}: {}", String.format("%02X", type), e.getMessage());
            return new Ed2kTag(type, nameId, null);
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
