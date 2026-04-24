package network.protocol.kad;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import network.node.NodeInfo;

/**
 * Codec Netty para mensajes Kademlia sobre UDP.
 * Se encarga de transformar objetos KadMessage en paquetes de bytes (DatagramPacket) y viceversa.
 * Sigue el formato oficial de eMule para garantizar la compatibilidad.
 */
public class KadCodec extends MessageToMessageCodec<DatagramPacket, KadMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(KadCodec.class);

    /**
     * ENCODE: Transforma un objeto KadMessage en bytes para ser enviados por la red.
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, KadMessage msg, List<Object> out) throws Exception {
        
        // Reservamos un buffer para construir el mensaje
        ByteBuf buf = ctx.alloc().buffer();
        
        // 1. Escribimos el Magic Byte de Kademlia (0xE4)
        buf.writeByte(0xE4); // KADEMLIAHEADER obligatorio en eMule!!! (1 byte)
        
        // 2. Escribimos el Opcode
        buf.writeByte(msg.getType().getOpcode());
        
        // 3. Escribimos el KadID del emisor (16 bytes)
        buf.writeBytes(msg.getSenderId().getBytes());

        // 4. Escribimos el cuerpo específico según el tipo de mensaje (v2)
        switch (msg.getType()) {

            case HELLO_REQ -> {
                KadMessage.HelloRequest req = (KadMessage.HelloRequest) msg;
                buf.writeShortLE(req.getReceiverPort()); // Puerto (LE)
                buf.writeByte(req.getVersion());         // Versión
                buf.writeByte(0);                        // Conteo de Tags (0) - ¡OBLIGATORIO en v2!
            }

            case HELLO_RES -> {
                KadMessage.HelloResponse res = (KadMessage.HelloResponse) msg;
                buf.writeShortLE(res.getReceiverPort()); // Puerto (LE)
                buf.writeByte(res.getVersion());         // Versión
                buf.writeByte(0);                        // Conteo de Tags (0) - ¡OBLIGATORIO en v2!
            }

            case FIND_NODE -> {
                KadMessage.FindNodeRequest req = (KadMessage.FindNodeRequest) msg;
                buf.writeBytes(req.getTargetId().getBytes());
            }

            case FIND_NODE_RES -> {
                KadMessage.FindNodeResponse res = (KadMessage.FindNodeResponse) msg;
                List<NodeInfo> nodes = res.getClosestNodes();
                buf.writeByte(Math.min(nodes.size(), 40));

                for (NodeInfo node : nodes) {
                    buf.writeBytes(node.getNodeId().getBytes());
                    buf.writeBytes(node.getAddress().getAddress());
                    buf.writeShortLE(node.getUdpPort());
                    buf.writeShortLE(node.getTcpPort());
                    buf.writeByte(node.getVersion());
                }
            }

            case PING, PONG -> {
                buf.writeLong(msg.getMessageId());
            }

            default -> { /* No payload */ }
        }

        // 5. Envío
        if (msg.getRecipient() != null) {
            out.add(new DatagramPacket(buf, msg.getRecipient()));
        } else {
            buf.release();
            logger.warn("Mensaje {} sin destinatario", msg.getType());
        }
    }

    /**
     * DECODE: Transforma bytes recibidos de la red en objetos KadMessage.
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        ByteBuf in = packet.content();
        
        // Validación mínima: Magic(1) + Opcode(1) + ID(16) = 18 bytes
        if (in.readableBytes() < 18) return; 

        // 1. Validamos el Magic Byte
        int magic = in.readUnsignedByte();

        if (magic != 0xE4) {
            // Si no empieza por 0xE4, no es un paquete Kad válido (o está comprimido, no soportado aún)
            return; 
        }

        // 2. Leemos el Opcode y verificamos si está comprimido (bit 0x40)
        int opcode = in.readUnsignedByte();
        boolean compressed = (opcode & 0x40) != 0;
        if (compressed) {
            opcode &= ~0x40; // Quitamos el bit de compresión para identificar el mensaje real
        }

        ByteBuf payload = null;
        try {
            // Si está comprimido, intentamos descomprimir
            if (compressed) {
                try {
                    byte[] compressedData = new byte[in.readableBytes()];
                    in.readBytes(compressedData);
                    byte[] decompressedData = decompress(compressedData);
                    payload = ctx.alloc().buffer(decompressedData.length);
                    payload.writeBytes(decompressedData);
                } catch (Exception e) {
                    // Si falla la descompresión (ej: aMule usando bit 0x40 para otra cosa)
                    // volvemos al buffer original como fallback.
                    in.readerIndex(2); // Volvemos a después del Magic y Opcode
                    payload = in.retain();
                    logger.debug("Fallo al descomprimir opcode 0x{}. Usando modo plano.", Integer.toHexString(opcode));
                }
            } else {
                payload = in.retain(); 
            }

            // 3. Leemos el KadID del remitente (siempre presente tras el opcode)
            if (payload.readableBytes() < 16) return;
            KadId senderId = new KadId(readBytes(payload, 16));

            // 4. Identificamos el tipo de mensaje
            KadMessage.Type type = KadMessage.Type.fromOpcode(opcode);

            if (type == null) {
                logger.warn("Opcode desconocido recibido: 0x{} (Comprimido: {})", 
                    Integer.toHexString(opcode), compressed);
                return;
            }

            KadMessage msg = null;
            
            switch (type) {

                case HELLO_REQ -> {
                    int port = payload.readUnsignedShortLE();
                    int version = payload.readUnsignedByte();
                    if (payload.readableBytes() >= 1) payload.readUnsignedByte(); // Leer conteo de tags (y saltar)
                    msg = new KadMessage.HelloRequest(senderId, port, version);
                }

                case HELLO_RES, HELLO_RES_ACK -> {
                    int port = payload.readUnsignedShortLE();
                    int version = payload.readUnsignedByte();
                    if (payload.readableBytes() >= 1) payload.readUnsignedByte(); // Leer conteo de tags (y saltar)
                    
                    if (type == KadMessage.Type.HELLO_RES) {
                        msg = new KadMessage.HelloResponse(senderId, port, version);
                    } else {
                        // Podríamos crear una clase específica, pero de momento HelloResponse nos sirve
                        msg = new KadMessage.HelloResponse(senderId, port, version);
                    }
                }

                case FIND_NODE -> {
                    // KADEMLIA2_REQ (0x21): 16 bytes ID objetivo
                    if (payload.readableBytes() >= 16) {
                        KadId targetId = new KadId(readBytes(payload, 16));
                        msg = new KadMessage.FindNodeRequest(senderId, 0, targetId); // Sin MessageID en v2
                    }
                }

                case FIND_NODE_RES -> {
                    // KADEMLIA2_RES (0x29): 1 byte count + nodos (25 bytes cada uno)
                    if (payload.readableBytes() >= 1) {
                        int count = payload.readUnsignedByte();
                        List<NodeInfo> nodes = new ArrayList<>();
                        
                        for (int i = 0; i < count; i++) {
                            if (payload.readableBytes() < 25) break; // ID(16)+IP(4)+UDP(2)+TCP(2)+Ver(1)

                            KadId id = new KadId(readBytes(payload, 16)); 
                            byte[] ipBytes = readBytes(payload, 4); 
                            int udpPort = payload.readUnsignedShortLE(); // Puerto UDP (Little Endian)
                            int tcpPort = payload.readUnsignedShortLE(); // Puerto TCP (Little Endian)
                            int version = payload.readUnsignedByte();    // Versión

                            InetAddress addr = InetAddress.getByAddress(ipBytes);
                            nodes.add(new NodeInfo(id, addr, udpPort, tcpPort, version));
                        }
                        msg = new KadMessage.FindNodeResponse(senderId, 0, nodes);
                    }
                }

                case PING, PONG -> {
                    // PING/PONG (v1 legacy): Siguen usando MessageID de 8 bytes
                    if (payload.readableBytes() >= 8) {
                        long messageId = payload.readLong();
                        if (type == KadMessage.Type.PING) {
                            msg = new KadMessage.PingRequest(senderId, messageId);
                        } else {
                            msg = new KadMessage.PingResponse(senderId, messageId);
                        }
                    }
                }

                default -> logger.warn("Tipo de mensaje desconocido recibido: {}", type);
            }

            // 5. Si hemos podido reconstruir el mensaje, le asignamos la dirección de origen
            if (msg != null) {
                msg.setSenderAddress(packet.sender()); // Guardamos quién envió el paquete UDP
                out.add(msg);
            }
        } finally {
            if (payload != null) payload.release(); // ¡IMPORTANTE! Liberamos el buffer para evitar fugas de memoria
        }
    }

    /**
     * Método auxiliar para leer un número fijo de bytes del buffer.
     * @param in Buffer del cual leer
     * @param length Número de bytes a leer
     * @return Array de bytes leído
     */
    private byte[] readBytes(ByteBuf in, int length) {
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return bytes;
    }

    /**
     * Descomprime un array de bytes usando Zlib (Inflater).
     * @param data Datos comprimidos
     * @return Datos descomprimidos
     */
    private byte[] decompress(byte[] data) throws Exception {

        // En eMule, el payload comprimido NO incluye el header de Zlib (cmf/flg)
        // Por lo tanto, usamos el parámetro 'nowrap' = true para indicarle 
        // al Inflater que el buffer empieza directamente con los datos comprimidos.
        
        Inflater inflater = new Inflater(true); 

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            inflater.setInput(data);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) break;
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } finally {
            inflater.end(); // Libera recursos nativos de la JVM
        }
    }
}
