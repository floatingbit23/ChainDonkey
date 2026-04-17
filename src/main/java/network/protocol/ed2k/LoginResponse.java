package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Representa el mensaje eD2K OP_LOGINRESPONSE (0x4C).
 *
 * Este mensaje es enviado por un servidor a un cliente tras un inicio de sesión exitoso.
 * Asigna principalmente al cliente su ID para la sesión. Un High ID indica
 * que el cliente es accesible por otros, mientras que un Low ID indica que está tras un cortafuegos (firewalled).
 */

public class LoginResponse extends Ed2kMessage {

    private final long clientId;

    /**
     * Construye un LoginResponse con el ID de cliente asignado.
     * @param clientId el ID asignado al cliente
     */
    public LoginResponse(long clientId) {
        super(Ed2kConstants.OP_LOGINRESPONSE);
        this.clientId = clientId;
    }

    /**
     * Getter. Devuelve el ID de cliente asignado.
     * @return el ID numérico del cliente
     */
    public long getClientId() {
        return clientId;
    }
    
    /**
     * Determina si el ID asignado se considera un High ID (es decir, accesible).
     * En eD2K, cualquier ID mayor o igual a 16777216 es un High ID.
     *
     * @return {@code true} si es High ID; {@code false} en caso contrario.
     */
    public boolean isHighId() {
        return clientId >= 16777216L;
    }

    // Coders y decoders

    /**
     * Codifica el mensaje en un buffer de bytes.
     * @param out el buffer de bytes donde codificar el mensaje
     */
    @Override
    public void encode(ByteBuf out) {
        out.writeIntLE((int) clientId); // Escribimos el ID
    }

    /**
     * Decodifica un ByteBuf en una nueva instancia de LoginResponse.
     *
     * @param in el ByteBuf del cual leer
     * @return una nueva instancia de LoginResponse poblada con el ID asignado
     */
    public static LoginResponse decode(ByteBuf in) {
        long id = in.readUnsignedIntLE(); // Leemos el ID
        return new LoginResponse(id); // Creamos y devolvemos el objeto
    }
}

