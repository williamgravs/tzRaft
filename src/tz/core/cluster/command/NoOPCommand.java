package tz.core.cluster.command;

import tz.base.common.Buffer;
import tz.core.cluster.state.Response;
import tz.core.msg.Encoder;

/**
 * "No operation" command for internal state machine
 * Leader uses this command to ensure its leadership
 */
public class NoOPCommand extends Command
{
    public static final int TYPE = 0;

    /**
     * Create new NoOPCommand
     */
    public NoOPCommand()
    {
        encode();
    }

    /**
     * Create new NoOPCommand
     * @param buf encoded no op command
     * @param len encoded length of the command
     */
    public NoOPCommand(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Encode the command
     */
    @Override
    void encode()
    {
        if (!rawReady) {
            int len = Encoder.byteLen(TYPE);

            rawMsg = new Buffer(len + Encoder.varIntLen(len));

            rawMsg.putVarInt(len);
            rawMsg.put(TYPE);

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

    }

    /**
     * Get encoded length
     * @return encoded length of the command
     */
    @Override
    public int encodedLen()
    {
        return Encoder.byteLen(TYPE) +
               Encoder.varIntLen(Encoder.byteLen(TYPE));
    }

    /**
     * Call handler of the command
     * @param handler command handler
     */
    @Override
    public Response execute(CommandExecutor handler)
    {
        return handler.executeNoOPCommand(this);
    }
}
