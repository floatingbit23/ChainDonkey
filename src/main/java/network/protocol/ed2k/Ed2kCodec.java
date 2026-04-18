package network.protocol.ed2k;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

/**
 * Codificador y decodificador para el protocolo eD2K.
 * Maneja la serialización y deserialización de mensajes eD2K.
 */
public class Ed2kCodec extends ByteToMessageCodec<Ed2kMessage> {

    private static final Logger logger = LoggerFactory.getLogger(Ed2kCodec.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        logger.info("[CODEC] Intentando decodificar. Bytes disponibles: {}", in.readableBytes());
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();
        boolean foundMagic = false;
        while (in.readableBytes() >= 5) {
            byte magicByte = in.readByte();
            if (magicByte == Ed2kConstants.PR_EDONKEY || magicByte == Ed2kConstants.PR_EMULE) {
                foundMagic = true;
                break;
            }
            // Si no es magic, seguimos buscando (skip)
        }

        if (!foundMagic) {
            // No hemos encontrado magic en lo que hay disponible
            return;
        }
        
        // Ya hemos leído el magicByte, ahora leemos la longitud
        int length = in.readIntLE();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte opcode = in.readByte();
        ByteBuf payload = in.readSlice(length - 1);
        logger.info("Recibido opcode eD2K: 0x{}", String.format("%02X", opcode));

        try {
            Ed2kMessage message = switch (opcode) {
                case Ed2kConstants.OP_LOGINREQUEST -> LoginRequest.decode(payload);
                case Ed2kConstants.OP_LOGINRESPONSE, Ed2kConstants.OP_IDCHANGE -> LoginResponse.decode(payload);
                case Ed2kConstants.OP_SERVERSTATUS -> ServerStatusMessage.decode(payload);
                case Ed2kConstants.OP_SERVERMESSAGE -> ServerMessage.decode(payload);
                default -> null;
            };

            if (message != null) {
                out.add(message);
            } else {
                logger.warn("Opcode eD2K desconocido o no soportado: 0x{}", String.format("%02X", opcode));
            }
        } catch (Exception e) {
            logger.error("Error al decodificar mensaje eD2K (opcode 0x{}): {}", String.format("%02X", opcode), e.getMessage(), e);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Ed2kMessage msg, ByteBuf out) throws Exception {
        out.writeByte(Ed2kConstants.PR_EDONKEY);
        int lengthPos = out.writerIndex();
        out.writeIntLE(0); // placeholder
        int startPos = out.writerIndex();
        out.writeByte(msg.getOpcode());
        msg.encode(out);
        int length = out.writerIndex() - startPos;
        out.setIntLE(lengthPos, length);
    }
}
