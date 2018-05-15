package tz.core.cluster.command;

import tz.base.common.Buffer;
import tz.core.cluster.state.Response;
import tz.core.msg.Encoder;

/**
 * Unregister command for the internal state machine
 * This command is used for client registration
 */
public class UnregisterCommand extends Command
{
    public static final int TYPE = 3;

    private int id;


    /**
     * Create new UnregisterCommand
     * @param id id to unregister
     */
    public UnregisterCommand(int id)
    {
        this.id = id;

        encode();
    }

    /**
     * Create new UnregisterCommand
     * @param buf encoded register command
     * @param len encoded command length
     */
    public UnregisterCommand(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Get the id
     * @return id
     */
    public int getId()
    {
        return id;
    }

    /**
     * Encode the command
     */
    @Override
    public void encode()
    {
        if (!rawReady) {
            int len = Encoder.byteLen(UnregisterCommand.TYPE)
                        + Encoder.varIntLen(id);

            rawMsg = new Buffer(len + Encoder.varIntLen(len));

            rawMsg.putVarInt(len);
            rawMsg.put(RegisterCommand.TYPE);
            rawMsg.putVarInt(id);

            rawMsg.flip();
            rawReady = true;
        }
    }

    /**
     * Decode the command
     */
    @Override
    void decode()
    {
        id = rawMsg.getVarInt();
    }

    /**
     * Calculate encoded length
     * @return encoded length of the command
     */
    @Override
    public int encodedLen()
    {
        int len = Encoder.byteLen(RegisterCommand.TYPE)
                    + Encoder.varIntLen(id);

        return len + Encoder.varIntLen(len);
    }

    /**
     * Call the command handler
     * @param handler command handler
     */
    @Override
    public Response execute(CommandExecutor handler)
    {
        return handler.executeUnregisterCommand(this);
    }
}
