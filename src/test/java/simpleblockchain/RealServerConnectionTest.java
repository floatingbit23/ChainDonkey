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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import network.node.NodeIdentity;
import network.protocol.ed2k.Ed2kCodec;
import network.protocol.ed2k.Ed2kConstants;
import network.protocol.ed2k.Ed2kMessage;
import network.protocol.ed2k.Ed2kObfuscationHandler;
import network.protocol.ed2k.Ed2kTag;
import network.protocol.ed2k.LoginRequest;
import network.protocol.ed2k.LoginResponse;
import network.protocol.ed2k.ServerMessage;
import network.protocol.ed2k.ServerStatusMessage;

/**
 * Test de integración real que intenta conectarse a un servidor de eMule público.
 * Este test verifica que nuestro protocolo es compatible con servidores reales.
 */
public class RealServerConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(RealServerConnectionTest.class);
    
    // eMule Sunrise (IP reportada activa)
    private static final String SERVER_IP = "176.123.5.89";
    private static final int SERVER_PORT = 4725;

    @Test
    public void testConnectToRealServer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final LoginResponse[] receivedResponse = {null};

        EventLoopGroup group = new NioEventLoopGroup();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        
        final Wallet wallet = new Wallet();
        final NodeIdentity identity = new NodeIdentity(wallet);
        
        try {
            // --- 1. Listener local en el puerto 4662 (Con Ofuscación para el Callback) ---
            ServerBootstrap sb = new ServerBootstrap();
            sb.group(bossGroup, group)
              .channel(NioServerSocketChannel.class)
              .childHandler(new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(SocketChannel ch) {
                      logger.info("[LISTENER] Recibida conexión entrante desde {} (callback del servidor)", ch.remoteAddress());
                      ch.pipeline().addLast(new Ed2kObfuscationHandler(false)); // Modo RESPONDER
                      ch.pipeline().addLast(new Ed2kCodec());
                      ch.pipeline().addLast(new SimpleChannelInboundHandler<Ed2kMessage>() {
                          @Override
                          public void channelActive(ChannelHandlerContext ctx) {
                              logger.info("[LISTENER] Handshake de ofuscación completado. Enviando OP_HELLO...");
                              
                              List<Ed2kTag> helloTags = new ArrayList<>();
                              helloTags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_STRING, Ed2kConstants.CT_NAME, "ChainDonkey_Alpha"));
                              helloTags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_VERSION, 60));
                              helloTags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_EMULE_VERSION, 0x003C0100));
                              
                              LoginRequest hello = new LoginRequest(
                                      identity.getUserHash(),
                                      0, 4662, helloTags
                              );
                              ctx.writeAndFlush(hello);
                          }
                          @Override
                          protected void channelRead0(ChannelHandlerContext ctx, Ed2kMessage msg) {
                              logger.info("[LISTENER] Recibido mensaje del servidor en el puerto 4662: {}", msg.getClass().getSimpleName());
                          }
                          @Override
                          public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                              logger.warn("[LISTENER] Error en el callback: {}", cause.getMessage());
                              ctx.close();
                          }
                      });
                  }
              });
            ChannelFuture listenerFuture = sb.bind(4662).sync();
            logger.info("[LISTENER] Escuchando en el puerto 4662 con soporte de ofuscación.");

            // --- 2. Conexión saliente ---
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                      ch.pipeline().addLast(new Ed2kObfuscationHandler(true)); // Modo INITIATOR
                     ch.pipeline().addLast(new Ed2kCodec());
                     ch.pipeline().addLast(new SimpleChannelInboundHandler<Ed2kMessage>() {
                         @Override
                         public void channelActive(ChannelHandlerContext ctx) {
                             logger.info("[TEST] Conectado al servidor {}:{}. Enviando LoginRequest...", SERVER_IP, SERVER_PORT);
                             
                             List<Ed2kTag> tags = new ArrayList<>();
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_STRING, Ed2kConstants.CT_NAME, "ChainDonkey_Alpha"));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_VERSION, 60));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_EMULE_VERSION, 0x003C0100));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_SERVER_FLAGS, 0x1D));

                             LoginRequest login = new LoginRequest(
                                     identity.getUserHash(),
                                     0, 4662, tags
                             );
                             ctx.writeAndFlush(login);
                         }

                         @Override
                         protected void channelRead0(ChannelHandlerContext ctx, Ed2kMessage msg) {
                             if (msg instanceof LoginResponse res) {
                                 String type = res.isHighId() ? "HighID" : "LowID";
                                 logger.info("[TEST] ID asignado: {} ({})", res.getClientId(), type);
                                 receivedResponse[0] = res;
                                 latch.countDown();
                             } else if (msg instanceof ServerStatusMessage status) {
                                 logger.info("[SERVER STATUS] Usuarios: {}, Archivos: {}", status.getUserCount(), status.getFileCount());
                             } else if (msg instanceof ServerMessage serverMsg) {
                                 logger.warn("[SERVER MESSAGE] El servidor dice: {}", serverMsg.getMessage());
                             }
                         }

                         @Override
                         public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                             logger.error("[TEST] Error en la conexión: {}", cause.getMessage());
                             ctx.close();
                             latch.countDown();
                         }

                         @Override
                         public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                             logger.warn("[TEST] La conexión TCP fue cerrada por el servidor.");
                             latch.countDown();
                             super.channelInactive(ctx);
                         }
                     });
                 }
             });

            logger.info("[TEST] Iniciando intento de conexión a {}:{}...", SERVER_IP, SERVER_PORT);
            ChannelFuture f = b.connect(SERVER_IP, SERVER_PORT).sync();
            
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            
            if (!completed) {
                logger.warn("[TEST] El servidor no respondió a tiempo o la conexión falló.");
            }

            f.channel().close().sync();
            listenerFuture.channel().close().sync();

        } finally {
            group.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

        assertNotNull(receivedResponse[0], "No se recibió respuesta del servidor real");
        assertTrue(receivedResponse[0].getClientId() > 0, "El ID de cliente recibido debería ser mayor que 0");
    }
}
