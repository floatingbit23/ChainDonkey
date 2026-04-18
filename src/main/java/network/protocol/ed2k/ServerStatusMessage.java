package network.protocol.ed2k;

import io.netty.buffer.ByteBuf;

/**
 * Representa el mensaje eD2K OP_SERVERSTATUS (0x40).
 *
 * Este mensaje es enviado por el servidor para informar sobre el estado global
 * (número de usuarios y archivos compartidos).
 */
public class ServerStatusMessage extends Ed2kMessage {

    private final long userCount;
    private final long fileCount;

    public ServerStatusMessage(long userCount, long fileCount) {
        super(Ed2kConstants.OP_SERVERSTATUS);
        this.userCount = userCount;
        this.fileCount = fileCount;
    }

    public long getUserCount() {
        return userCount;
    }

    public long getFileCount() {
        return fileCount;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeIntLE((int) userCount);
        out.writeIntLE((int) fileCount);
    }

    public static ServerStatusMessage decode(ByteBuf in) {
        long users = in.readUnsignedIntLE();
        long files = in.readUnsignedIntLE();
        return new ServerStatusMessage(users, files);
    }
}
