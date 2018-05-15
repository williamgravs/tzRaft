package tz.core.cluster.command;

import tz.base.common.Buffer;
import tz.base.record.ClusterRecord;
import tz.core.cluster.state.Response;
import tz.core.msg.Encoder;

/**
 * Command for cluster record changes
 */
public class ConfigCommand extends Command
{
    public static final int TYPE = 4;

    private ClusterRecord record;

    /**
     * Create new ConfigCommand
     * @param record cluster record
     */
    public ConfigCommand(ClusterRecord record)
    {
        this.record = record;

        encode();
    }

    /**
     * Create new ConfigCommand
     * @param buf raw encoded command
     * @param len raw encoded length
     */
    public ConfigCommand(Buffer buf, int len)
    {
        super(buf, len);

        decode();
        rawMsg.rewind();
        rawReady = true;
    }

    /**
     * Get the record
     * @return cluster record
     */
    public ClusterRecord getRecord()
    {
        return record;
    }

    /**
     * Encode the command to a raw buffer
     */
    @Override
    void encode()
    {
        if (!rawReady) {
            int len = Encoder.byteLen(RegisterCommand.TYPE)
                      + record.rawLen();

            rawMsg = new Buffer(len + Encoder.varIntLen(len));

            rawMsg.putVarInt(len);
            rawMsg.put(ConfigCommand.TYPE);
            record.encode(rawMsg);

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
        record = new ClusterRecord(rawMsg);
    }

    /**
     * Calculate encoded length of the command
     * @return encoded length of this command
     */
    @Override
    public int encodedLen()
    {
        int len = Encoder.byteLen(RegisterCommand.TYPE) + record.rawLen();

        return len + Encoder.varIntLen(len);
    }

    /**
     * Call handler of the command
     * @param handler command handler
     */
    @Override
    public Response execute(CommandExecutor handler)
    {
        return handler.executeConfigCommand(this);
    }
}
