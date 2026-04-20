package network.protocol.ed2k;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

/**
 * Manejador de la ofuscación del protocolo eD2K (Handshake Diffie-Hellman + RC4).
 * Configurado para conexiones a Servidores (Sunrise, eMule Security) usando sales 0xCB/0x22.
 */
public class Ed2kObfuscationHandler extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(Ed2kObfuscationHandler.class);

    private static final BigInteger G = BigInteger.valueOf(2);
    // Prime dh768_p real (96 bytes)
    private static final BigInteger P = new BigInteger(1, new byte[]{
        (byte)0xF2, (byte)0xBF, (byte)0x52, (byte)0xC5, (byte)0x5F, (byte)0x58, (byte)0x7A, (byte)0xDD, (byte)0x53, (byte)0x71, (byte)0xA9, (byte)0x36,
        (byte)0xE8, (byte)0x86, (byte)0xEB, (byte)0x3C, (byte)0x62, (byte)0x17, (byte)0xA3, (byte)0x3E, (byte)0xC3, (byte)0x4C, (byte)0xB4, (byte)0x0D,
        (byte)0xC7, (byte)0x3A, (byte)0x41, (byte)0xA6, (byte)0x43, (byte)0xAF, (byte)0xFC, (byte)0xE7, (byte)0x21, (byte)0xFC, (byte)0x28, (byte)0x63,
        (byte)0x66, (byte)0x53, (byte)0x5B, (byte)0xDB, (byte)0xCE, (byte)0x25, (byte)0x9F, (byte)0x22, (byte)0x86, (byte)0xDA, (byte)0x4A, (byte)0x91,
        (byte)0xB2, (byte)0x07, (byte)0xCB, (byte)0xAA, (byte)0x52, (byte)0x55, (byte)0xD4, (byte)0xF6, (byte)0x1C, (byte)0xCE, (byte)0xAE, (byte)0xD4,
        (byte)0x5A, (byte)0xD5, (byte)0xE0, (byte)0x74, (byte)0x7D, (byte)0xF7, (byte)0x78, (byte)0x18, (byte)0x28, (byte)0x10, (byte)0x5F, (byte)0x34,
        (byte)0x0F, (byte)0x76, (byte)0x23, (byte)0x87, (byte)0xF8, (byte)0x8B, (byte)0x28, (byte)0x91, (byte)0x42, (byte)0xFB, (byte)0x42, (byte)0x68,
        (byte)0x8F, (byte)0x05, (byte)0x15, (byte)0x0F, (byte)0x54, (byte)0x8B, (byte)0x5F, (byte)0x43, (byte)0x6A, (byte)0xF7, (byte)0x0D, (byte)0xF3
    });

    private static final int PRIMESIZE_BYTES = 96;
    private static final int MAGICVALUE_SYNC = 0x835E6FC4;
    
    // Sales para Servidor
    private static final byte SERVER_SALT_RECV = (byte) 0xCB; // 203
    private static final byte SERVER_SALT_SEND = (byte) 0x22; // 34

    // Sales para Peer (Cliente-Cliente) - Iguales a Servidor según eMule source code (MAGICVALUE_REQUESTER=34, MAGICVALUE_SERVER=203)
    private static final byte PEER_SALT_SEND = (byte) 0x22; // MAGICVALUE_REQUESTER = 34 → clave de envío de Client A
    private static final byte PEER_SALT_RECV = (byte) 0xCB; // MAGICVALUE_SERVER = 203    → clave de recepción de Client A

    public enum State {
        INITIATOR_SEND_DH_PUB,
        INITIATOR_WAIT_DH_ANSWER,
        RESPONDER_WAIT_DH_PUB,
        WAIT_HANDSHAKE_START,
        WAIT_MAGIC,
        WAIT_METHODS,
        WAIT_PADDING,
        ENCRYPTING
    }

    private State state;
    private final boolean initiator;
    private final byte[] targetUserHash; // Si es null, usamos modo Servidor (DH)
    private Cipher rc4Send;
    private Cipher rc4Receive;
    private BigInteger privateA;
    private byte[] randomKeyPart; 
    private final SecureRandom random = new SecureRandom();

    public Ed2kObfuscationHandler() {
        this(true, null);
    }

    public Ed2kObfuscationHandler(boolean initiator) {
        this(initiator, null);
    }

    public Ed2kObfuscationHandler(boolean initiator, byte[] targetUserHash) {
        this.initiator = initiator;
        this.targetUserHash = targetUserHash;
        this.state = initiator ? State.INITIATOR_SEND_DH_PUB : State.WAIT_HANDSHAKE_START;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (initiator) {
            startInitiatorHandshake(ctx);
        } else {
            logger.info("[OBFUSCATION] Modo RESPONDER activo.");
            super.channelActive(ctx);
        }
    }

    private void startInitiatorHandshake(ChannelHandlerContext ctx) {
        // SI TENEMOS HASH, USAMOS PROTOCOLO PEER (SIN DH)
        if (targetUserHash != null) {
            startPeerInitiatorHandshake(ctx);
            return;
        }

        // SI NO HAY HASH, SEGUIMOS EL FLUJO DH DE SIEMPRE (SERVIDOR)
        logger.info("---------------------------------------------------------");
        logger.info("[OBFUSCATION] Iniciando Handshake Diffie-Hellman (Iniciador)...");
        logger.info("---------------------------------------------------------");
        byte[] aBytes = new byte[16];
        random.nextBytes(aBytes);
        privateA = new BigInteger(1, aBytes);
        BigInteger publicA = G.modPow(privateA, P);
        ByteBuf out = ctx.alloc().buffer();
        out.writeByte(0x80);
        out.writeBytes(encodeTo96Bytes(publicA));
        ctx.writeAndFlush(out);
        state = State.INITIATOR_WAIT_DH_ANSWER;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case INITIATOR_SEND_DH_PUB -> {} // Solo enviamos, no esperamos recibir
            case RESPONDER_WAIT_DH_PUB -> {
                if (in.readableBytes() < PRIMESIZE_BYTES + 1) return;
                in.readByte(); // 0x80
                byte[] publicBBytes = new byte[PRIMESIZE_BYTES];
                in.readBytes(publicBBytes);
                BigInteger publicB = new BigInteger(1, publicBBytes);
                byte[] aBytes = new byte[16];
                random.nextBytes(aBytes);
                privateA = new BigInteger(1, aBytes);
                BigInteger publicA = G.modPow(privateA, P);
                BigInteger sharedSecret = publicB.modPow(privateA, P);
                initCiphers(encodeTo96Bytes(sharedSecret), false);
                
                ByteBuf dhAnswer = ctx.alloc().buffer(PRIMESIZE_BYTES);
                dhAnswer.writeBytes(encodeTo96Bytes(publicA));
                ctx.writeAndFlush(dhAnswer);
                
                state = State.WAIT_MAGIC;
                decode(ctx, in, out);
            }
            case INITIATOR_WAIT_DH_ANSWER -> {
                if (in.readableBytes() < PRIMESIZE_BYTES) return;
                byte[] publicBBytes = new byte[PRIMESIZE_BYTES];
                in.readBytes(publicBBytes);
                BigInteger publicB = new BigInteger(1, publicBBytes);
                BigInteger sharedSecret = publicB.modPow(privateA, P);
                initCiphers(encodeTo96Bytes(sharedSecret), true);
                
                // Enviar Magic inmediatamente
                logger.info("[OBFUSCATION] DH compartido derivado. Enviando Magic Value...");
                sendHandshakeMagic(ctx);
                
                state = State.WAIT_MAGIC;
                decode(ctx, in, out);
            }
            case WAIT_HANDSHAKE_START -> {
                if (in.readableBytes() < 1) return;
                byte first = in.getByte(in.readerIndex());
                logger.info("[OBFUSCATION] Primer byte recibido: 0x{}", String.format("%02X", first));
                
                // Si el primer byte es un opcode eD2K estándar (0xE3, 0xC5, 0xD4),
                // el servidor nos está hablando en PLANO. Nos retiramos.
                if (first == (byte)0xE3 || first == (byte)0xC5 || first == (byte)0xD4) {
                    logger.info("[OBFUSCATION] Detectado texto PLANO. Retirando handler de ofuscación.");
                    ctx.pipeline().remove(this);
                    return;
                }

                if (first == 0x01 || first == 0x02 || first == 0x03) {
                    state = State.RESPONDER_WAIT_DH_PUB;
                    decode(ctx, in, out);
                }
            }
            case WAIT_MAGIC -> {
                if (in.readableBytes() < 4) return;
                byte[] magicBytes = new byte[4];
                in.getBytes(in.readerIndex(), magicBytes);
                logger.debug("[OBFUSCATION] Bytes crudos recibidos (esperando Magic): {}", io.netty.buffer.ByteBufUtil.hexDump(magicBytes));
                
                in.readBytes(magicBytes);
                // Magic del servidor a pos 1024 (descarte inicial en initCiphers)
                byte[] decryptedMagic = rc4Receive.update(magicBytes);
                long magic = ((decryptedMagic[3] & 0xFFL) << 24) | 
                             ((decryptedMagic[2] & 0xFFL) << 16) | 
                             ((decryptedMagic[1] & 0xFFL) << 8) | 
                             (decryptedMagic[0] & 0xFFL);
                
                if (magic != (MAGICVALUE_SYNC & 0xFFFFFFFFL)) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : decryptedMagic) sb.append(String.format("%02X ", b));
                    logger.error("[OBFUSCATION] Magic mismatch at pos 1024! Recibido 0x{} (Bytes: {})", Long.toHexString(magic).toUpperCase(), sb.toString());
                    throw new IllegalStateException("Obfuscation Magic Value mismatch at pos 1024!");
                }
                
                logger.info("[OBFUSCATION] >>> Magic del servidor verificado (pos 1024).");
                state = State.WAIT_METHODS;
                decode(ctx, in, out);
            }
            case WAIT_METHODS -> {
                int bytesToRead = (targetUserHash != null) ? 2 : 3; // Peer: 2 bytes, Servidor: 3 bytes
                if (in.readableBytes() < bytesToRead) return;
                
                byte[] block = new byte[bytesToRead];
                in.readBytes(block);
                byte[] decBlock = rc4Receive.update(block);
                
                // El primer byte es el método seleccionado (0x00 = RC4)
                int method = decBlock[0] & 0xFF;
                // El último byte siempre es el PaddingLen
                int paddingLen = decBlock[bytesToRead - 1] & 0xFF;
                
                logger.info("[OBFUSCATION] Negociación recibida: Método {} ({} bytes), Padding {} bytes", 
                    method == 0 ? "0x00 (RC4)" : "0x" + Integer.toHexString(method),
                    bytesToRead, paddingLen);
                
                if (paddingLen > 0) {
                    if (in.readableBytes() < paddingLen) {
                        in.resetReaderIndex();
                        return;
                    }
                    byte[] padding = new byte[paddingLen];
                    in.readBytes(padding);
                    rc4Receive.update(padding);
                }
                
                state = State.WAIT_PADDING;
                decode(ctx, in, out);
            }
            case WAIT_PADDING -> {
                // El padding ya se procesó en WAIT_METHODS si existía
                logger.info("[OBFUSCATION] #########################################################");
                logger.info("[OBFUSCATION] # Handshake Completado. ¡Túnel RC4 Sincronizado!");
                logger.info("[OBFUSCATION] #########################################################");
                
                state = State.ENCRYPTING;
                ctx.fireChannelActive();
                decode(ctx, in, out);
            }
            case ENCRYPTING -> {
                if (in.readableBytes() > 0) {
                    byte[] enc = new byte[in.readableBytes()];
                    in.readBytes(enc);
                    byte[] dec = rc4Receive.update(enc);
                    out.add(ctx.alloc().buffer(dec.length).writeBytes(dec));
                }
            }
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (state != State.ENCRYPTING) {
            out.writeBytes(msg);
            return;
        }
        byte[] plain = new byte[msg.readableBytes()];
        msg.readBytes(plain);
        out.writeBytes(rc4Send.update(plain));
    }

    private void startPeerInitiatorHandshake(ChannelHandlerContext ctx) {
        logger.info("[OBFUSCATION] Iniciando Handshake Peer-to-Peer con eMule...");
        
        randomKeyPart = new byte[4];
        random.nextBytes(randomKeyPart);
        
        // Inicializar Ciphers de Peer
        rc4Receive = new Cipher(derivePeerKey(targetUserHash, PEER_SALT_RECV, randomKeyPart));
        rc4Receive.update(new byte[1024]);

        rc4Send = new Cipher(derivePeerKey(targetUserHash, PEER_SALT_SEND, randomKeyPart));
        rc4Send.update(new byte[1024]);

        // Enviar Paquete Inicial Peer (12 bytes)
        ByteBuf handshake = ctx.alloc().buffer(12);
        handshake.writeByte(0x80); 
        handshake.writeBytes(randomKeyPart);
        
        ByteBuf plain = ctx.alloc().buffer(7);
        plain.writeIntLE(MAGICVALUE_SYNC);
        plain.writeByte(0x01); // MethodsSupported (Bitmask: bit 0 = ENM_OBFUSCATION)
        plain.writeByte(0x00); // MethodPreferred (ENM_OBFUSCATION = 0x00)
        plain.writeByte(0x00); // PaddingLen
        
        byte[] plainBytes = new byte[7];
        plain.readBytes(plainBytes);
        handshake.writeBytes(rc4Send.update(plainBytes));
        
        ctx.writeAndFlush(handshake);
        plain.release();
        
        state = State.WAIT_MAGIC;
    }

    private byte[] derivePeerKey(byte[] userHash, byte salt, byte[] randomPart) {
        byte[] buffer = new byte[userHash.length + 1 + randomPart.length];
        System.arraycopy(userHash, 0, buffer, 0, userHash.length);
        buffer[userHash.length] = salt;
        System.arraycopy(randomPart, 0, buffer, userHash.length + 1, randomPart.length);
        return new MD5Sum(buffer).GetRawHash();
    }

    private void sendHandshakeMagic(ChannelHandlerContext ctx) {
        ByteBuf out = ctx.alloc().buffer();
        
        // 1. Magic (4 bytes) - Pos 0-3
        out.writeIntLE(MAGICVALUE_SYNC);
        
        // 2. Negociación (2 bytes) - Pos 4-5
        out.writeByte(0x00); // Protocolo eDonkey
        out.writeByte(0x00); // Padding Length 0
        
        byte[] plain = new byte[out.readableBytes()];
        out.readBytes(plain);
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(rc4Send.update(plain)));
        out.release();
    }



    private void initCiphers(byte[] sBytes, boolean isInitiator) {
        // Para servidores, el salt es simplemente el Shared Secret (96) + Flag (1)
        byte[] buffer = new byte[PRIMESIZE_BYTES + 1];
        System.arraycopy(sBytes, 0, buffer, 0, PRIMESIZE_BYTES);
        
        // Clave de Recepción
        buffer[PRIMESIZE_BYTES] = isInitiator ? SERVER_SALT_RECV : (byte)0x00;
        rc4Receive = new Cipher(new MD5Sum(buffer).GetRawHash());
        rc4Receive.update(new byte[1024]); // El Salto de 1024 bytes

        // Clave de Envío
        buffer[PRIMESIZE_BYTES] = isInitiator ? SERVER_SALT_SEND : (byte)0x01;
        rc4Send = new Cipher(new MD5Sum(buffer).GetRawHash());
        rc4Send.update(new byte[1024]); // El Salto Único Inicial
        
        logger.info("[OBFUSCATION] Motores RC4 inicializados con el Salto de Oro de 1024 bytes.");
    }

    private byte[] encodeTo96Bytes(BigInteger n) {
        byte[] bytes = n.toByteArray();
        byte[] res = new byte[96];
        if (bytes.length > 96) System.arraycopy(bytes, bytes.length - 96, res, 0, 96);
        else System.arraycopy(bytes, 0, res, 96 - bytes.length, bytes.length);
        return res;
    }

    private static class MD5Sum {
        private final byte[] hash;
        public MD5Sum(byte[] data) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                hash = md.digest(data);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        public byte[] GetRawHash() { return hash; }
    }

    private static class Cipher {
        private final byte[] S = new byte[256];
        private int i = 0, j = 0;
        public Cipher(byte[] key) {
            for (int k = 0; k < 256; k++) S[k] = (byte) k;
            int j2 = 0;
            for (int k = 0; k < 256; k++) {
                j2 = (j2 + (S[k] & 0xFF) + (key[k % key.length] & 0xFF)) & 0xFF;
                byte temp = S[k];
                S[k] = S[j2];
                S[j2] = temp;
            }
        }
        public byte[] update(byte[] data) {
            byte[] out = new byte[data.length];
            for (int k = 0; k < data.length; k++) {
                i = (i + 1) & 0xFF;
                j = (j + (S[i] & 0xFF)) & 0xFF;
                byte temp = S[i];
                S[i] = S[j];
                S[j] = temp;
                int t = ((S[i] & 0xFF) + (S[j] & 0xFF)) & 0xFF;
                out[k] = (byte) (data[k] ^ S[t]);
            }
            return out;
        }
    }
}
