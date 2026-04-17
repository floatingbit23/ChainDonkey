package simpleblockchain;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import network.protocol.ed2k.Ed2kCodec;
import network.protocol.ed2k.Ed2kConstants;
import network.protocol.ed2k.Ed2kTag;
import network.protocol.ed2k.LoginRequest;

/**
 * Pruebas unitarias para verificar la correcta codificación y decodificación
 * del protocolo eD2K utilizando el Ed2kCodec.
 */
public class Ed2kProtocolTest {

    @Test
    public void testLoginRequestRoundtrip() {
        // 1. Preparar datos de prueba
        byte[] userHash = new byte[16];
        for (int i = 0; i < 16; i++) userHash[i] = (byte) i;
        
        long clientId = 12345678L;
        int port = 4662;
        
        List<Ed2kTag> tags = new ArrayList<>();
        tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_STRING, Ed2kConstants.CT_NAME, "ChainDonkeyNode"));
        tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_VERSION, 60));

        LoginRequest originalMsg = new LoginRequest(userHash, clientId, port, tags);

        // 2. Configurar el canal embebido con nuestro Códec
        EmbeddedChannel channel = new EmbeddedChannel(new Ed2kCodec());

        // 3. ENCODE: Escribir el objeto en el canal (Java -> Bytes)
        assertTrue(channel.writeOutbound(originalMsg));
        ByteBuf encoded = channel.readOutbound();

        assertNotNull(encoded);
        
        // Verificaciones básicas del encabezado eD2K
        assertEquals(Ed2kConstants.PR_EDONKEY, encoded.readByte(), "Magic Byte incorrecto");
        encoded.readIntLE(); // Leemos la longitud (sin usar la variable para evitar el warning)
        assertEquals(Ed2kConstants.OP_LOGINREQUEST, encoded.readByte(), "Opcode incorrecto");

        // 4. DECODE: Pasar los bytes de vuelta por el canal (Bytes -> Java)
        // Reiniciamos el índice de lectura del buffer para que el decode pueda leerlo desde el principio
        encoded.readerIndex(0); 
        
        assertTrue(channel.writeInbound(encoded));
        LoginRequest decodedMsg = channel.readInbound();

        // 5. Verificaciones finales
        assertNotNull(decodedMsg);
        assertEquals(originalMsg.getOpcode(), decodedMsg.getOpcode());
        assertArrayEquals(userHash, decodedMsg.getUserHash(), "El hash de usuario no coincide");
        assertEquals(clientId, decodedMsg.getClientId(), "El ID de cliente no coincide");
        assertEquals(port, decodedMsg.getPort(), "El puerto no coincide");
        
        // Verificar etiquetas
        assertEquals(originalMsg.getTags().size(), decodedMsg.getTags().size(), "El número de etiquetas no coincide");
        assertEquals("ChainDonkeyNode", decodedMsg.getTags().get(0).getValue());
        assertEquals(60L, ((Number)decodedMsg.getTags().get(1).getValue()).longValue());

        channel.finish();
    }

    @Test
    public void testInvalidMagicByte() {
        EmbeddedChannel channel = new EmbeddedChannel(new Ed2kCodec());
        
        ByteBuf garbage = Unpooled.buffer();
        garbage.writeByte(0xAA); // Magic byte inválido
        garbage.writeIntLE(10);
        garbage.writeByte(0x01);
        
        // Intentar decodificar basura
        channel.writeInbound(garbage);
        
        // No debería haber producido ningún mensaje decodificado
        Object msg = channel.readInbound();
        assertNull(msg, "No debería haberse decodificado nada de un Magic Byte inválido");
        
        channel.finish();
    }
}
