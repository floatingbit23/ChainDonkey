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
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();
        byte magicByte = in.readByte();
        if (magicByte != Ed2kConstants.PR_EDONKEY && magicByte != Ed2kConstants.PR_EMULE) {
            in.resetReaderIndex();
            return;
        }
        
        int length = in.readIntLE();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte opcode = in.readByte();
        ByteBuf payload = in.readSlice(length - 1);

        try {
            Ed2kMessage message = switch (opcode) {
                case Ed2kConstants.OP_LOGINREQUEST -> LoginRequest.decode(payload);
                case Ed2kConstants.OP_LOGINRESPONSE, Ed2kConstants.OP_IDCHANGE -> LoginResponse.decode(payload);
                case Ed2kConstants.OP_SERVERSTATUS -> ServerStatusMessage.decode(payload);
                case Ed2kConstants.OP_SERVERMESSAGE -> ServerMessage.decode(payload);
                case Ed2kConstants.OP_SECIDENTSTATE -> Ed2kSecureIdent.decode(payload);
                case Ed2kConstants.OP_SIGNATURE -> Ed2kSignature.decode(payload);
                case Ed2kConstants.OP_PUBLICKEY -> Ed2kPublicKey.decode(payload);
                case Ed2kConstants.OP_GETSOURCES_EXT -> SourceRequest.decode(payload);
                case Ed2kConstants.OP_FOUNDSOURCES, Ed2kConstants.OP_FOUNDSOURCES_EMULE, Ed2kConstants.OP_ANSWERSOURCES2 -> SourceResponse.decode(payload, opcode);
                case Ed2kConstants.OP_STARTUPLOADREQ -> StartUploadRequest.decode(payload);
                default -> new GenericMessage(opcode, payload);
            };

            if (message != null) {
                out.add(message);
            }
        } catch (Exception e) {
            logger.error("Error al decodificar mensaje eD2K (opcode 0x{}): {}", String.format("%02X", opcode), e.getMessage());
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Ed2kMessage msg, ByteBuf out) throws Exception {
        byte protocol = Ed2kConstants.PR_EDONKEY;
        
        // Mensajes que requieren protocolo eMule (0xC5)
        if (msg.getOpcode() == Ed2kConstants.OP_SECIDENTSTATE || 
            msg.getOpcode() == Ed2kConstants.OP_SIGNATURE ||
            msg.getOpcode() == Ed2kConstants.OP_PUBLICKEY ||
            msg.getOpcode() == Ed2kConstants.OP_GETSOURCES_EXT ||
            msg.getOpcode() == (byte)0x83) {
            protocol = Ed2kConstants.PR_EMULE;
        }
        
        out.writeByte(protocol);
        int lengthPos = out.writerIndex();
        out.writeIntLE(0); // placeholder
        int startPos = out.writerIndex();
        out.writeByte(msg.getOpcode());
        msg.encode(out);
        int length = out.writerIndex() - startPos;
        out.setIntLE(lengthPos, length);
    }
}
