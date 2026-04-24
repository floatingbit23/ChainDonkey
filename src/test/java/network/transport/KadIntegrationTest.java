package network.transport;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import network.protocol.ed2k.Ed2kIdentityManager;
import network.protocol.kad.KadCodec;
import network.protocol.kad.KadId;
import network.protocol.kad.KadMessage;

public class KadIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(KadIntegrationTest.class);

    @Test
    public void testKadHelloToEmule() throws Exception {

        // Parámetros de un nodo eMule real
        String host = "45.149.183.49";
        int port = 52232;

        logger.info("---------------------------------------------------------");
        logger.info("[KAD INTEGRATION TEST] Iniciando saludo Kad a eMule...");
        logger.info("[TARGET] {}:{} (UDP)", host, port);
        logger.info("---------------------------------------------------------");

        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            Ed2kIdentityManager identity = new Ed2kIdentityManager();
            KadId myKadId = new KadId(identity.getUserHash());

            logger.info("[TEST] Mi KadId: {}", myKadId.toString());

            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioDatagramChannel.class)
             .handler(new ChannelInitializer<NioDatagramChannel>() {
                  @Override
                  protected void initChannel(NioDatagramChannel ch) {
                      ChannelPipeline p = ch.pipeline();
                      
                      // SNIFFER: Loguea los bytes crudos antes de que el KadCodec los toque
                      p.addLast("sniffer", new SimpleChannelInboundHandler<DatagramPacket>() {
                          @Override
                          protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                              ByteBuf content = msg.content();
                              byte[] data = new byte[content.readableBytes()];
                              content.getBytes(0, data);
                              StringBuilder sb = new StringBuilder();
                              for (byte b : data) sb.append(String.format("%02X ", b));
                              logger.info("[RAW DATA] Recibidos {} bytes de {}: {}", data.length, msg.sender(), sb.toString());
                              ctx.fireChannelRead(msg.retain());
                          }
                      });

                      p.addLast(new KadCodec());
                      p.addLast(new SimpleChannelInboundHandler<KadMessage>() {
                          @Override
                          public void channelActive(ChannelHandlerContext ctx) throws Exception {
                              super.channelActive(ctx);
                              logger.info("[TEST] Canal UDP conectado. Enviando KADEMLIA2_HELLO_REQ...");
                              
                              KadMessage.HelloRequest hello = new KadMessage.HelloRequest(myKadId, 16003, 8);
                              hello.setRecipient(new InetSocketAddress(host, port));
                              
                              ctx.writeAndFlush(hello);
                              logger.info("[TEST] KADEMLIA2_HELLO_REQ enviado a {}:{}", host, port);
                          }

                          @Override
                          protected void channelRead0(ChannelHandlerContext ctx, KadMessage msg) {
                              if (msg == null) return;

                              if (msg instanceof KadMessage.HelloResponse) {
                                  logger.info("[SUCCESS] ¡Recibido KADEMLIA2_HELLO_RES de eMule!");
                              } else if (msg instanceof KadMessage.HelloRequest) {
                                  logger.info("[SUCCESS] ¡Recibido KADEMLIA2_HELLO_REQ de eMule! El nodo nos está saludando de vuelta.");
                                  KadMessage.HelloResponse res = new KadMessage.HelloResponse(myKadId, 16003, 8);
                                  res.setRecipient(msg.getSenderAddress());
                                  ctx.writeAndFlush(res);
                              } else {
                                  logger.info("[INFO] Mensaje Kad recibido: {}", msg.getType());
                              }
                          }
                          
                          @Override
                          public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                              logger.error("[ERROR] Excepción en canal Kad: ", cause);
                          }
                      });
                  }
              });

            ChannelFuture f = b.connect(host, port).sync();
            
            if (f.isSuccess()) {
                logger.info("[TEST] UDP Channel bound/connected.");
                logger.info("[INFO] Esperando 10 segundos para ver si eMule responde...");
                Thread.sleep(10000);
            }

        } finally {
            group.shutdownGracefully();
        }
    }
}
