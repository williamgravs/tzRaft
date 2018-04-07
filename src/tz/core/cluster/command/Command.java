package tz.core.cluster.command;

import tz.base.common.Buffer;
import tz.core.cluster.state.Response;

import java.nio.ByteBuffer;

/**
 * Abstract command class for state machine operations
 */
public abstract class Command
{
    protected int length;
    protected Buffer rawMsg;
    protected boolean rawReady;

    /**
     * Create new Command
     */
    protected Command()
    {

    }

    /**
     * Create new Command
     *
     * @param buf Encoded buffer of the command
     * @param len Length of the encoded command
     */
    protected Command(Buffer buf, int len)
    {
        this.rawMsg = buf;
        this.length = len;
    }

    /**
     * Decode an encoded command
     *
     * @param buf Encoded buffer of the command
     * @return    Decoded command as an object
     *
     * @throws UnsupportedOperationException if command type is unknown
     */
    public static Command create(Buffer buf)
    {
        int len  = buf.getVarInt();
        int type = buf.get();

        switch (type) {
            case NoOPCommand.TYPE:
                return new NoOPCommand();
            case RegisterCommand.TYPE:
                return new RegisterCommand(buf, len);
            case UnregisterCommand.TYPE:
                return new UnregisterCommand(buf, len);
            case ConfigCommand.TYPE:
                return new ConfigCommand(buf, len);

            default:
                throw new UnsupportedOperationException("Unknown msg type : " + type);
        }
    }

    /**
     * Get encoded command
     *
     * @return Encoded command
     */
    public ByteBuffer getRaw()
    {
        return rawMsg.backend();
    }

    /**
     * Encode the command
     */
    abstract void encode();

    /**
     * Decode the command
     */
    abstract void decode();

    /**
     * Get encoded length of the command
     * @return Encoded length
     */
    abstract int encodedLen();

    /**
     * Execute command
     * @param executor Command executer
     * @return         Response
     */
    public abstract Response execute(CommandExecutor executor);
}
