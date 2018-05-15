package tz.core.cluster.command;

import tz.base.common.Buffer;
import tz.base.record.NodeRecord;
import tz.core.cluster.state.Response;
import tz.core.msg.Encoder;

/**
 * Register command for the internal state machine
 * This command is used for client registration
 */
public class RegisterCommand extends Command
{
    public static final int TYPE = 2;

    private String name;

    public RegisterCommand(String name)
    {
        this.name = name;

        encode();
    }

    /**
     * Create new RegisterCommand
     * @param buf encoded register command
     * @param len encoded command length
     */
    public RegisterCommand(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Encode the command
     */
    @Override
    void encode()
    {
        if (!rawReady) {
            int len = Encoder.byteLen(RegisterCommand.TYPE) +
                      Encoder.stringLen(name);

            rawMsg = new Buffer(len + Encoder.varIntLen(len));

            rawMsg.putVarInt(len);
            rawMsg.put(RegisterCommand.TYPE);
            rawMsg.putString(name);

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
        name = rawMsg.getString();
    }

    /**
     * Calculate encoded length
     * @return encoded length of the command
     */
    @Override
    public int encodedLen()
    {
        int len = Encoder.byteLen(RegisterCommand.TYPE) +
                  Encoder.stringLen(name);

        return len + Encoder.varIntLen(len);
    }

    /**
     * Call the command handler
     * @param handler command handler
     */
    @Override
    public Response execute(CommandExecutor handler)
    {
        return handler.executeRegisterCommand(this);
    }
}
