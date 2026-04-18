package network.protocol.ed2k;

import java.math.BigInteger;
import java.net.InetSocketAddress;
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
    
    // Sales para Servidor (Confirmadas por Brute-Force en Sunrise)
    private static final byte SERVER_SALT_RECV = (byte) 0xCB; // 203
    private static final byte SERVER_SALT_SEND = (byte) 0x22; // 34

    public enum State {
        INITIATOR_SEND_DH_PUB,
        INITIATOR_WAIT_DH_ANSWER,
        RESPONDER_WAIT_DH_PUB,
        WAIT_MAGIC,
        WAIT_METHODS,
        WAIT_PADDING,
        ENCRYPTING
    }

    private State state;
    private final boolean initiator;
    private Cipher rc4Send;
    private Cipher rc4Receive;
    private BigInteger privateA;
    private int expectedPadding = 0;
    private final SecureRandom random = new SecureRandom();

    public Ed2kObfuscationHandler() {
        this(true);
    }

    public Ed2kObfuscationHandler(boolean initiator) {
        this.initiator = initiator;
        this.state = initiator ? State.INITIATOR_SEND_DH_PUB : State.RESPONDER_WAIT_DH_PUB;
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
        logger.info("[OBFUSCATION] Iniciando Handshake Diffie-Hellman (Iniciador)...");
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
                initCiphers(ctx, encodeTo96Bytes(sharedSecret), false);
                
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
                initCiphers(ctx, encodeTo96Bytes(sharedSecret), true);
                
                // Enviar Magic inmediatamente
                sendHandshakeMagic(ctx, true);
                
                state = State.WAIT_MAGIC;
                decode(ctx, in, out);
            }
            case WAIT_MAGIC -> {
                if (in.readableBytes() < 4) return;
                byte[] magicBytes = new byte[4];
                in.readBytes(magicBytes);
                byte[] decryptedMagic = rc4Receive.update(magicBytes);
                long magic = ((decryptedMagic[3] & 0xFFL) << 24) | 
                             ((decryptedMagic[2] & 0xFFL) << 16) | 
                             ((decryptedMagic[1] & 0xFFL) << 8) | 
                             (decryptedMagic[0] & 0xFFL);
                
                if (magic != (MAGICVALUE_SYNC & 0xFFFFFFFFL)) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : decryptedMagic) sb.append(String.format("%02X ", b));
                    logger.error("[OBFUSCATION] Magic mismatch! Recibido 0x{} (Bytes: {})", Long.toHexString(magic).toUpperCase(), sb.toString());
                    throw new IllegalStateException("Obfuscation Magic Value mismatch!");
                }
                
                state = State.WAIT_METHODS;
                decode(ctx, in, out);
            }
            case WAIT_METHODS -> {
                int bytesWanted = initiator ? 2 : 3;
                if (in.readableBytes() < bytesWanted) return;
                byte[] methodsBytes = new byte[bytesWanted];
                in.readBytes(methodsBytes);
                byte[] dec = rc4Receive.update(methodsBytes);
                expectedPadding = dec[bytesWanted - 1] & 0xFF;
                state = State.WAIT_PADDING;
                decode(ctx, in, out);
            }
            case WAIT_PADDING -> {
                if (in.readableBytes() < expectedPadding) return;
                if (expectedPadding > 0) {
                    byte[] padding = new byte[expectedPadding];
                    in.readBytes(padding);
                    rc4Receive.update(padding);
                }
                
                if (!initiator) sendHandshakeMagic(ctx, false);
                
                logger.info("[OBFUSCATION] Handshake completado exitosamente.");
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

    private void sendHandshakeMagic(ChannelHandlerContext ctx, boolean isInit) {
        ByteBuf out = ctx.alloc().buffer();
        out.writeIntLE(MAGICVALUE_SYNC);
        if (isInit) {
            out.writeByte(0x01); // 1 método
            out.writeByte(0x01); // RC4
        } else {
            out.writeByte(0x01); // RC4 seleccionado
        }
        int paddingLen = random.nextInt(32);
        out.writeByte(paddingLen);
        if (paddingLen > 0) {
            byte[] padding = new byte[paddingLen];
            random.nextBytes(padding);
            out.writeBytes(padding);
        }
        
        byte[] plain = new byte[out.readableBytes()];
        out.readBytes(plain);
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(rc4Send.update(plain)));
    }

    private void initCiphers(ChannelHandlerContext ctx, byte[] sBytes, boolean isInitiator) {
        // Para servidores, el salt es simplemente el Shared Secret (96) + Flag (1)
        byte[] buffer = new byte[PRIMESIZE_BYTES + 1];
        System.arraycopy(sBytes, 0, buffer, 0, PRIMESIZE_BYTES);
        
        // Clave de Recepción
        buffer[PRIMESIZE_BYTES] = isInitiator ? SERVER_SALT_RECV : (byte)0x00;
        rc4Receive = new Cipher(new MD5Sum(buffer).GetRawHash());
        rc4Receive.update(new byte[1024]); // Descarte de 1024 bytes (Confirmado por brute-force)

        // Clave de Envío
        buffer[PRIMESIZE_BYTES] = isInitiator ? SERVER_SALT_SEND : (byte)0x01;
        rc4Send = new Cipher(new MD5Sum(buffer).GetRawHash());
        rc4Send.update(new byte[1024]); // Descarte de 1024 bytes
        
        logger.info("[OBFUSCATION] Cifrados inicializados con sales 0x{} (Recv) / 0x{} (Send)", 
            Integer.toHexString(isInitiator ? SERVER_SALT_RECV & 0xFF : 0),
            Integer.toHexString(isInitiator ? SERVER_SALT_SEND & 0xFF : 1));
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
