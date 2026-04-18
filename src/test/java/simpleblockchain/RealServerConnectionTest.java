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
import io.netty.buffer.ByteBuf;
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
    
    // Servidor por defecto (eMule Sunrise)
    private static final String DEFAULT_SERVER_IP = "176.123.5.89";
    private static final int DEFAULT_SERVER_PORT = 4725;

    @Test
    public void testConnectToRealServer() throws InterruptedException {
        // Leemos configuración desde propiedades del sistema para permitir tests dinámicos
        String serverIp = System.getProperty("server.ip", DEFAULT_SERVER_IP);
        int serverPort = Integer.getInteger("server.port", DEFAULT_SERVER_PORT);
        
        logger.info("[TEST] Configuración: {}:{}", serverIp, serverPort);
        
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
                              logger.info("[LISTENER] Conexión establecida. Esperando OP_HELLO del servidor...");
                          }
                          @Override
                          protected void channelRead0(ChannelHandlerContext ctx, Ed2kMessage msg) {
                              logger.info("[LISTENER] Recibido mensaje del servidor en el puerto 4662: {}", msg.getClass().getSimpleName());
                              
                              if (msg instanceof LoginRequest) {
                                  LoginRequest hello = (LoginRequest) msg;
                                  logger.info("[LISTENER] OP_HELLO recibido. Hash: {}, ID: {}, Tags: {}", 
                                          io.netty.buffer.ByteBufUtil.hexDump(hello.getUserHash()),
                                          hello.getClientId(),
                                          hello.getTags().size());
                                  
                                  for (Ed2kTag tag : hello.getTags()) {
                                      logger.info("[LISTENER]   Tag: 0x{} = {}", String.format("%02X", tag.getName()), tag.getValue());
                                  }

                                  logger.info("[LISTENER] Enviando OP_HELLOANSWER (0x4C) como respuesta...");
                                  
                                  ByteBuf respBuf = ctx.alloc().buffer();
                                  respBuf.writeByte(0xE3); // Protocolo
                                  int lengthPos = respBuf.writerIndex();
                                  respBuf.writeIntLE(0); // Placeholder longitud
                                  int start = respBuf.writerIndex();

                                  respBuf.writeByte(0x4C); // OP_HELLOANSWER (ID CHANGE)
                                  respBuf.writeBytes(identity.getUserHash());
                                  respBuf.writeIntLE(0); // Client ID nulo
                                  respBuf.writeShortLE(4662); // Nuestro puerto
                                  respBuf.writeIntLE(3); // 3 Tags

                                  new Ed2kTag(Ed2kConstants.TAG_TYPE_STRING, Ed2kConstants.CT_NAME, "ChainDonkey_Alpha").writeToBuffer(respBuf);
                                  new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_VERSION, 60).writeToBuffer(respBuf);
                                  new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_EMULE_VERSION, 0x003C0100).writeToBuffer(respBuf);

                                  // Calculamos y escribimos la longitud real
                                  respBuf.setIntLE(lengthPos, respBuf.writerIndex() - start);

                                  // Usamos context.channel().writeAndFlush para saltarnos el codec (ya está codificado)
                                  ctx.channel().writeAndFlush(respBuf).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
                              }
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
                      
                      // Raw Byte Logger para depurar qu? nos env?a el servidor antes del codec
                      ch.pipeline().addLast(new io.netty.channel.ChannelInboundHandlerAdapter() {
                          @Override
                          public void channelRead(ChannelHandlerContext ctx, Object msg) {
                              if (msg instanceof io.netty.buffer.ByteBuf buf) {
                                  byte[] bytes = new byte[Math.min(buf.readableBytes(), 32)];
                                  buf.getBytes(buf.readerIndex(), bytes);
                                  logger.info("[MAIN RECV] Bytes crudos (32b): {}", io.netty.buffer.ByteBufUtil.hexDump(bytes));
                              }
                              ctx.fireChannelRead(msg);
                          }
                      });
                     ch.pipeline().addLast(new Ed2kCodec());
                     ch.pipeline().addLast(new SimpleChannelInboundHandler<Ed2kMessage>() {
                         @Override
                          public void channelActive(ChannelHandlerContext ctx) {
                              logger.info("[TEST] Configuracion: {}:{}", serverIp, serverPort);
                              logger.info("[TEST] Conectado al servidor. Enviando LoginRequest...");
                             
                             List<Ed2kTag> tags = new ArrayList<>();
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_STRING, Ed2kConstants.CT_NAME, "ChainDonkey_Alpha"));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_VERSION, 60));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_EMULE_VERSION, 0x003C0100));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT16, Ed2kConstants.CT_PORT, 4662));
                             tags.add(new Ed2kTag(Ed2kConstants.TAG_TYPE_UINT32, Ed2kConstants.CT_SERVER_FLAGS, 0xFFFFFFFF));

                             LoginRequest login = new LoginRequest(
                                     identity.getUserHash(),
                                     0, 4662, tags
                             );
                             ctx.writeAndFlush(login);
                         }

                         @Override
                         protected void channelRead0(ChannelHandlerContext ctx, Ed2kMessage msg) {
                             logger.info("[TEST] Mensaje eD2K recibido: 0x{}", String.format("%02X", msg.getOpcode()));
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

            logger.info("[TEST] Iniciando intento de conexión a {}:{}...", serverIp, serverPort);
            ChannelFuture f = b.connect(serverIp, serverPort).sync();
            
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            
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
