package network.protocol.ed2k;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

/**
 * Representa el mensaje eD2K OP_LOGINREQUEST (0x01).
 *
 * Este mensaje es enviado por un cliente a un servidor inmediatamente después de establecer
 * una conexión TCP para autenticarse y proporcionar sus parámetros de configuración.
 */

public class LoginRequest extends Ed2kMessage {

    private final byte[] userHash; // 16 bytes
    private final long clientId; // 4 bytes
    private final int port; // 2 bytes
    private final List<Ed2kTag> tags; // variable length

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

        out.writeBytes(userHash);
        out.writeIntLE((int) clientId);
        out.writeShortLE(port);
        out.writeIntLE(tags.size());

        // Codificamos cada tag
        for (Ed2kTag tag : tags) {
            tag.writeToBuffer(out); 
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

        // Leemos el número de tags
        int tagCount = in.readIntLE();

        // Instanciamos una lista para guardar los tags
        List<Ed2kTag> parsedTags = new ArrayList<>();

        // Leemos cada tag
        for (int i = 0; i < tagCount; i++) {
            parsedTags.add(Ed2kTag.readFromBuffer(in));
        }
        
        // Creamos y devolvemos el objeto
        return new LoginRequest(hash, clientIdentifier, clientPort, parsedTags);
    }
}

