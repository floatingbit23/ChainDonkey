package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Clase base abstracta para todos los mensajes dentro del protocolo eD2K.
 *
 * Cada mensaje tiene inherentemente un opcode que define su estructura y propósito.
 * Las subclases concretas son responsables de codificar payload específico.
 */

public abstract class Ed2kMessage {
    
    private final byte protocolByte;
    private final byte opcode;

    /**
     * Constructor base para un mensaje eD2K.
     * @param opcode el opcode que identifica el tipo de mensaje
     */
    protected Ed2kMessage(byte opcode) {
        this(Ed2kConstants.PR_EDONKEY, opcode);
    }

    /**
     * Constructor base para un mensaje eD2K con byte de protocolo específico.
     * @param protocolByte el byte de protocolo (Magic Byte)
     * @param opcode el opcode que identifica el tipo de mensaje
     */
    protected Ed2kMessage(byte protocolByte, byte opcode) {
        this.protocolByte = protocolByte;
        this.opcode = opcode;
    }

    /**
     * Obtiene el byte de protocolo (Magic Byte) de este mensaje.
     * @return el byte de protocolo (e.g. 0xE3 o 0xC5)
     */
    public byte getProtocolByte() {
        return protocolByte;
    }

    /**
     * Getter del opcode. Devuelve el opcode de este mensaje.
     * @return el opcode del mensaje
     */
    public byte getOpcode() {
        return opcode;
    }

    /**
     * Serializa el payload del mensaje en el buffer proporcionado. (No codifica el encabezado de 6 bytes (4 CRC + 2 Length), solo el payload específico)
     * @param out el ByteBuf en el cual escribir los datos del payload
     */
    public abstract void encode(ByteBuf out);
    

    /**
     * Decodifica el payload de un mensaje eD2K basado en el opcode.
     * @param opcode el opcode que identifica el tipo de mensaje
     * @param in el ByteBuf que contiene el payload del mensaje
     * @return la subclase de Ed2kMessage correctamente instanciada
     * @throws IllegalArgumentException si el opcode es desconocido
     */
    public static Ed2kMessage decode(byte opcode, ByteBuf in) {

        // Evaluamos el opcode y obtenemos el valor de forma distinta para cada opcode (Switch moderno, Java 14+)
        return switch (opcode) {

            case Ed2kConstants.OP_LOGINREQUEST -> LoginRequest.decode(in); // 0x01

            case Ed2kConstants.OP_LOGINRESPONSE -> LoginResponse.decode(in); // 0x02
            // Se añadirán más opcodes aquí

            default ->
                throw new IllegalArgumentException(String.format("Opcode eD2K desconocido: 0x%02X", opcode));
        };

    }
}

