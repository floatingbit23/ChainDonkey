package network.protocol.ed2k;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representa una etiqueta Type-Length-Value (TLV) utilizada en el protocolo eD2K.
 *
 * Las etiquetas se utilizan intensamente en el protocolo para adjuntar información opcional o metadatos
 * a los mensajes. Los datos se codifican en formato Little Endian.
 */
public class Ed2kTag {

    private static final Logger logger = LoggerFactory.getLogger(Ed2kTag.class);
    
    private final byte type;
    private final byte nameId;
    private final Object value; // nº bytes variable

    /**
     * Construye una nueva Ed2kTag.
     *
     * @param type   el byte de tipo de la etiqueta (ej. TAG_TYPE_UINT32)
     * @param nameId el identificador de nombre de la etiqueta (ej. CT_PORT)
     * @param value  la carga útil (payload) del valor de la etiqueta
     */

    public Ed2kTag(byte type, byte nameId, Object value) {
        this.type = type;
        this.nameId = nameId;
        this.value = value;
    }

    // --- Getters ---

    public byte getType() {
        return type;
    }

    public byte getNameId() {
        return nameId;
    }

    public Object getValue() {
        return value;
    }
    
    /**
     * Escribe esta Tag en un ByteBuf de Netty utilizando el formato eD2k Little Endian.
     * Metemos: type (1 byte), namelen (1 byte), nameId (1 byte), value (variable length)
     * 
     * @param out el búfer en el cual escribir
     */
    public void writeToBuffer(ByteBuf out) {

        out.writeByte(type); // Tipo de etiqueta (1 byte)
        
        // Las etiquetas pueden tener nombres enteros o nombres de cadena.
        // Por ahora, usamos solo nombres de enteros estándar de 1 byte.
        out.writeShortLE(1); // Namelen - longitud del ID del nombre/etiqueta
        out.writeByte(nameId); // ID del nombre/etiqueta
        
        // Según el tipo de etiqueta, codificamos el valor de forma diferente (Switch moderno, Java 14+):
        switch (type) {
            case Ed2kConstants.TAG_TYPE_UINT8 -> out.writeByte(((Number) value).intValue());

            case Ed2kConstants.TAG_TYPE_UINT16 -> out.writeShortLE(((Number) value).intValue());

            case Ed2kConstants.TAG_TYPE_UINT32 -> out.writeIntLE(((Number) value).intValue());

            case Ed2kConstants.TAG_TYPE_STRING -> {
                // Obtenemos el string en array de bytes en UTF-8
                byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                // Escribimos la longitud del string
                out.writeShortLE(strBytes.length);
                // Escribimos el string
                out.writeBytes(strBytes);
            }

            case Ed2kConstants.TAG_TYPE_FLOAT -> out.writeFloatLE(((Number) value).floatValue());

            case Ed2kConstants.TAG_TYPE_HASH -> {
                // Obtenemos el hash en array de bytes
                byte[] hashBytes = (byte[]) value;
                // Si el hash no es de 16 bytes, lanzamos excepción
                if (hashBytes.length != 16) {
                    throw new IllegalArgumentException("Las etiquetas de hash deben tener exactamente 16 bytes");
                }
                out.writeBytes(hashBytes); // Escribimos el hash si es de 16 bytes
            }

            default -> {
                // Por ahora no soportamos otros tipos de etiquetas
                logger.error("Intentando escribir un tipo de etiqueta eD2K no soportado: 0x{}", String.format("%02X", type));
                throw new UnsupportedOperationException("Tipo de etiqueta no soportado: " + type);
            }
        }

    }

    /**
     * Lee una etiqueta de un ByteBuf de Netty.
     *
     * @param in el búfer del cual leer
     * @return una Ed2kTag analizada
     */

    public static Ed2kTag readFromBuffer(ByteBuf in) {


        byte type = in.readByte();
        int nameLength = in.readUnsignedShortLE();
        byte nameId = -1; // inicializamos con un valor por defecto para saber si el nameId es válido o no

        // Si nameLength es 1, entonces el nameId es un byte. Si es mayor, es un string.
        // Por ahora solo implementamos el caso de que nameLength sea 1.
        if (nameLength == 1) {
            nameId = in.readByte(); 
        } else {
            // Omitir nombres no estándar por ahora
            logger.info("Omitiendo etiqueta eD2K con nombre de longitud no estándar: {} bytes", nameLength);
            in.skipBytes(nameLength); 
        }

        // Evaluamos el tipo de etiqueta y obtenemos el valor de forma distinta para cada tipo de etiqueta (Switch moderno, Java 14+)
        Object value = switch (type) {
            case Ed2kConstants.TAG_TYPE_UINT8 -> in.readUnsignedByte();

            case Ed2kConstants.TAG_TYPE_UINT16 -> in.readUnsignedShortLE();

            case Ed2kConstants.TAG_TYPE_UINT32 -> in.readUnsignedIntLE();

            case Ed2kConstants.TAG_TYPE_STRING -> {
                int strLen = in.readUnsignedShortLE();
                byte[] strBytes = new byte[strLen];
                in.readBytes(strBytes);
                yield new String(strBytes, StandardCharsets.UTF_8);
            }

            case Ed2kConstants.TAG_TYPE_FLOAT -> in.readFloatLE();

            case Ed2kConstants.TAG_TYPE_HASH -> {
                byte[] hashBytes = new byte[16];
                in.readBytes(hashBytes);
                yield hashBytes;
            }
            
            default -> {
                logger.error("Se encontró un tipo de etiqueta eD2K no soportado durante la lectura: 0x{}", String.format("%02X", type));
                throw new UnsupportedOperationException("Tipo de etiqueta no soportado durante la lectura: " + type);
            }
        };

        // Devolvemos una nueva etiqueta con el tipo, el nameId y el valor
        return new Ed2kTag(type, nameId, value);
    }
}
