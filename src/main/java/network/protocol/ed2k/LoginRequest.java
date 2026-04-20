package network.protocol.ed2k;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

/**
 * Representa el mensaje eD2K OP_LOGINREQUEST (0x01).
 *
 * Este mensaje es enviado por un cliente a un servidor inmediatamente después de establecer
 * una conexión TCP para autenticarse y proporcionar sus parámetros de configuración.
 */

public class LoginRequest extends Ed2kMessage {
    private static final Logger logger = LoggerFactory.getLogger(LoginRequest.class);

    private final byte[] userHash; // 16 bytes
    private final long clientId; // 4 bytes
    private final int port; // 2 bytes
    private final List<Ed2kTag> tags; // variable length
    private final long serverIp; // 4 bytes (opcional para P2P)
    private final int serverPort; // 2 bytes (opcional para P2P)
    private boolean isP2P = false; // Indica si es un mensaje entre clientes

    /**
     * Construye un LoginRequest con los parámetros de identidad proporcionados.
     *
     * @param userHash el hash MD4 que identifica al usuario
     * @param clientId el ID del cliente (0 si aún no se ha asignado un High ID)
     * @param port     el puerto TCP en el que el cliente escucha conexiones entrantes de pares
     * @param tags     una lista de etiquetas suplementarias (capabilities, nombre del cliente, versión, etc.)
     * @throws IllegalArgumentException si userHash no tiene exactamente 16 bytes
     */
    public LoginRequest(byte[] userHash, long clientId, int port, List<Ed2kTag> tags) {
        this(userHash, clientId, port, tags, 0, 0);
    }

    public LoginRequest(byte[] userHash, long clientId, int port, List<Ed2kTag> tags, long serverIp, int serverPort) {
        this(userHash, clientId, port, tags, serverIp, serverPort, false);
    }

    public LoginRequest(byte[] userHash, long clientId, int port, List<Ed2kTag> tags, long serverIp, int serverPort, boolean isP2P) {
        // Pasamos el opcode del mensaje a la clase padre
        super(Ed2kConstants.OP_LOGINREQUEST);

        // Chequeos de seguridad y validaciones
        if (userHash.length != 16) {
            throw new IllegalArgumentException("El userHash debe tener exactamente 16 bytes");
        }

        // Guardamos los datos
        this.userHash = userHash;
        this.clientId = clientId;
        this.port = port;
        this.tags = tags;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.isP2P = isP2P;
    }

    // Getters

    public byte[] getUserHash() {
        return userHash;
    }

    public long getClientId() {
        return clientId;
    }
 
    public int getPort() {
        return port;
    }

    /**
     * Obtiene la lista de etiquetas adjuntas.
     * @return las etiquetas asociadas con esta solicitud de inicio de sesión
     */
    public List<Ed2kTag> getTags() {
        return tags;
    }

    // Coders y decoders

    /**
     * Codifica el mensaje en un buffer de bytes (sin incluir CRC ni longitud).
     * @param out el buffer de bytes donde codificar el mensaje
     */

    @Override
    public void encode(ByteBuf out) {
        if (isP2P) {
            out.writeByte(16); // eMule P2P espera la longitud del hash (16) al principio
        }
        
        out.writeBytes(userHash);
        out.writeIntLE((int) clientId);
        out.writeShortLE(port);
        out.writeIntLE(tags.size());

        // Codificamos cada tag
        for (Ed2kTag tag : tags) {
            tag.writeToBuffer(out); 
        }

        if (isP2P) {
            // Para P2P añadimos la IP y Puerto del servidor al final (6 bytes)
            out.writeIntLE((int) serverIp);
            out.writeShortLE(serverPort);
        }
    }

    /**
     * Decodifica un ByteBuf en una nueva instancia de LoginRequest.
     *
     * @param in el ByteBuf del cual leer
     * @return una nueva instancia de LoginRequest poblada con los datos leídos
     */

    public static LoginRequest decode(ByteBuf in) {

        byte[] hash = new byte[16];
        in.readBytes(hash);
        
        long clientIdentifier = in.readUnsignedIntLE();
        int clientPort = in.readUnsignedShortLE();

        // Algunos servidores insertan un byte de control (ej. 0x12) antes del conteo de tags.
        // Si no lo saltamos, leemos el contador de tags desplazado y da valores enormes.
        if (in.readableBytes() > 0 && in.getByte(in.readerIndex()) == 0x12) {
            logger.info("Saltando byte de control 0x12 antes del conteo de tags");
            in.readByte();
        }

        // Leemos el número de tags (puede ser un entero de 4 bytes)
        int tagCount = in.readIntLE();

        // Instanciamos una lista para guardar los tags
        List<Ed2kTag> parsedTags = new ArrayList<>();

        // Leemos cada tag
        for (int i = 0; i < tagCount; i++) {
            Ed2kTag tag = Ed2kTag.readFromBuffer(in);
            if (tag != null) {
                parsedTags.add(tag);
            }
        }

        long sIp = 0;
        int sPort = 0;
        if (in.readableBytes() >= 6) {
            sIp = in.readUnsignedIntLE();
            sPort = in.readUnsignedShortLE();
        }
        
        // Creamos y devolvemos el objeto
        return new LoginRequest(hash, clientIdentifier, clientPort, parsedTags, sIp, sPort);
    }
}

