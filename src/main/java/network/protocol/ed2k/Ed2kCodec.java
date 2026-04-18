package network.protocol.ed2k;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

/**
 * Códec de Netty para codificar y decodificar mensajes eD2K sobre TCP.
 *
 * Este códec maneja la estructura de trama de mensaje estándar de eD2K:
 * 1 byte: Magic Byte del protocolo (p. ej., 0xE3 o 0xC5)
 * 4 bytes: Longitud del mensaje (excluyendo el encabezado de 6 bytes, little endian)
 * 1 byte: Opcode del mensaje que identifica el tipo de mensaje
 * Variable: Carga útil (payload) del mensaje
 */

public class Ed2kCodec extends ByteToMessageCodec<Ed2kMessage> {

    private static final Logger logger = LoggerFactory.getLogger(Ed2kCodec.class);

    /** Longitud total esperada del encabezado que precede a la carga útil. */
    private static final int HEADER_LENGTH = 6; // 1 Magic Byte + 4 Length bytes + 1 Opcode byte

    /**
     * Serializa el mensaje eD2K en el buffer proporcionado.
     * @param ctx el contexto del canal (no utilizado, heredado de ByteToMessageCodec para mantener firma del método padre)
     * @param msg el mensaje a serializar
     * @param out el buffer en el cual escribir los datos
     * @throws Exception si ocurre un error
     */

    @Override
    protected void encode(ChannelHandlerContext ctx, Ed2kMessage msg, ByteBuf out) throws Exception {

        // Guardamos el índice de inicio del mensaje
        int startIndex = out.writerIndex();

        // Reservamos espacio para el encabezado de 6 bytes
        out.writeZero(HEADER_LENGTH);

        // La subclase escribe su payload específico
        msg.encode(out);

        // Guardamos el índice final del mensaje
        int endIndex = out.writerIndex();
        
        // Calculamos la longitud del payload
        int payloadLength = endIndex - startIndex - HEADER_LENGTH;

        // Longitud del mensaje = longitud de la carga útil + 1 byte para el opcode
        int ed2kMessageLength = payloadLength + 1;

        // Volver atrás y escribir el encabezado real
        out.writerIndex(startIndex);
        out.writeByte(msg.getProtocolByte()); // Usar el byte de protocolo específico del mensaje
        out.writeIntLE(ed2kMessageLength); 
        out.writeByte(msg.getOpcode());

        // Restaurar el índice de escritura al final del paquete
        out.writerIndex(endIndex);

        if (logger.isDebugEnabled()) {
            byte[] packetBytes = new byte[endIndex - startIndex];
            out.getBytes(startIndex, packetBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : packetBytes) {
                hexString.append(String.format("%02X ", b));
            }
            logger.info("Enviando mensaje eD2K: {} (Opcode: 0x{}). Hex: {}", 
                msg.getClass().getSimpleName(), 
                String.format("%02X", msg.getOpcode()),
                hexString.toString());
        }
        
        // TEMPORARY: always print hex dump for debugging
        byte[] debugBytes = new byte[endIndex - startIndex];
        out.getBytes(startIndex, debugBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : debugBytes) {
            sb.append(String.format("%02X ", b));
        }
        System.out.println("[HEX DUMP] " + msg.getClass().getSimpleName() + " : " + sb.toString());
    }

    /**
     * Decodifica el mensaje eD2K en el buffer proporcionado.
     * @param ctx el contexto del canal
     * @param in el buffer en el cual leer los datos
     * @param out la lista de mensajes decodificados
     * @throws Exception si ocurre un error
     */

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        // Esperar hasta tener al menos el encabezado (6 bytes)
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }

        // Marcar el índice de lectura actual para poder volver si el mensaje es incompleto
        in.markReaderIndex();

        // Leemos el Magic Byte
        byte magicByte = in.readByte(); 
        
        logger.info("[DEBUG] Ed2kCodec.decode - Bytes disponibles: {}, MagicByte leído: 0x{}", in.readableBytes() + 1, String.format("%02X", magicByte));

        // Verificamos si el Magic Byte es correcto
        if (magicByte != Ed2kConstants.PR_EDONKEY && magicByte != Ed2kConstants.PR_EMULE) {
            // Si hay violación del protocolo, omitimos el Magic Byte e intentamos de nuevo en la siguiente sincronización.
            logger.warn("[DEBUG] Magic Byte inválido: 0x{}. Descartando 1 byte para realinear.", String.format("%02X", magicByte));
            in.resetReaderIndex();
            in.readByte(); // Descartar este byte
            return;
        }

        // Leemos la longitud del mensaje (Longitud de la carga útil + 1 byte para el opcode)
        int messageLength = in.readIntLE();

        // Nos aseguramos de tener el mensaje completo en el búfer (incluye el opcode)
        if (in.readableBytes() < messageLength) {
            in.resetReaderIndex(); // Si no tenemos el mensaje completo, volvemos al inicio y esperamos más datos.
            return;
        }

        byte opcode = in.readByte(); // Leemos el opcode.
        
        // Restamos 1 porque messageLength incluía el opcode que acabamos de leer
        int payloadLength = messageLength - 1;

        // Fragmentar el búfer para evitar que el decodificador de mensajes lea más allá de su payload
        ByteBuf payloadBuffer = in.readSlice(payloadLength);
        
        // try-catch para manejar posibles errores en el decodificador
        try {

            Ed2kMessage message = Ed2kMessage.decode(opcode, payloadBuffer);
            out.add(message);
            
            logger.info("Recibido mensaje eD2K: {} (Opcode: 0x{})", message.getClass().getSimpleName(), String.format("%02X", opcode));
            
        } catch (IllegalArgumentException e) {
            // Opcode desconocido, ignorar mensaje pero el índice del búfer avanza por readSlice
            logger.warn("Se recibió un mensaje eD2K con un opcode desconocido: 0x{}", String.format("%02X", opcode));
        }
    }
}

