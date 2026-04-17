package simpleblockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import network.node.NodeIdentity;
import network.protocol.ed2k.Ed2kCodec;
import network.protocol.ed2k.Ed2kConstants;
import network.protocol.ed2k.Ed2kMessage;
import network.protocol.ed2k.Ed2kTag;
import network.protocol.ed2k.LoginRequest;
import network.protocol.ed2k.LoginResponse;

/**
 * Test de integración real que intenta conectarse a un servidor de eMule público.
 * Este test verifica que nuestro protocolo es compatible con servidores reales.
 */
public class RealServerConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(RealServerConnectionTest.class);
    
    // IP de eMule Security (El más estable actualmente)
    private static final String SERVER_IP = "91.200.42.119";
    private static final int SERVER_PORT = 9939;

    @Test
    public void testConnectToRealServer() throws InterruptedException {
        // Usamos un Latch para esperar a la respuesta del servidor en un entorno asíncrono
        CountDownLatch latch = new CountDownLatch(1);
        final LoginResponse[] receivedResponse = {null};

        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new Ed2kCodec());
                     ch.pipeline().addLast(new SimpleChannelInboundHandler<Ed2kMessage>() {
                         @Override
                         public void channelActive(ChannelHandlerContext ctx) {
                             logger.info("[TEST] Conectado al servidor {}:{}. Enviando LoginRequest...", SERVER_IP, SERVER_PORT);
                             
                             // Creamos una identidad para el test
                             Wallet wallet = new Wallet();
                             NodeIdentity identity = new NodeIdentity(wallet);
                             
                             // Añadimos etiquetas obligatorias que los servidores reales suelen exigir
                             List<Ed2kTag> tags = new ArrayList<>();
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_STRING, Ed2kConstants.CT_NAME, "ChainDonkey_Alpha"));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_VERSION, 60)); // Versión 60 (eMule 0.50a)
                             
                             // Flags de capacidades (Soportar compresión, nuevas etiquetas, etc.)
                             // 0x1D es un valor estándar para un cliente moderno
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_SERVER_FLAGS, 0x1D));

                             // Enviamos el LoginRequest real con las etiquetas
                             LoginRequest login = new LoginRequest(
                                     identity.getUserHash(),
                                     0, // Client ID inicial
                                     4662, // Nuestro puerto TCP
                                     tags
                             );
                             ctx.writeAndFlush(login);
                         }

                         @Override
                         protected void channelRead0(ChannelHandlerContext ctx, Ed2kMessage msg) {
                             if (msg instanceof LoginResponse res) {
                                 logger.info("[TEST] ¡ÉXITO! Recibida respuesta del servidor. ID asignado: {}", res.getClientId());
                                 receivedResponse[0] = res;
                                 latch.countDown();
                                 ctx.close();
                             }
                         }

                         @Override
                         public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                             logger.error("[TEST] Error en la conexión: {}", cause.getMessage());
                             ctx.close();
                             latch.countDown();
                         }
                     });
                 }
             });

            logger.info("[TEST] Iniciando intento de conexión a {}:{}...", SERVER_IP, SERVER_PORT);
            b.connect(SERVER_IP, SERVER_PORT);
            
            // Esperamos un máximo de 10 segundos para la respuesta
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            
            if (!completed) {
                logger.warn("[TEST] El servidor no respondió a tiempo o la conexión falló.");
            }

            assertNotNull(receivedResponse[0], "No se recibió respuesta del servidor real (puede estar saturado o caído)");
            assertTrue(receivedResponse[0].getClientId() > 0, "El ID de cliente recibido debería ser mayor que 0");

        } finally {
            group.shutdownGracefully();
        }
    }
}
