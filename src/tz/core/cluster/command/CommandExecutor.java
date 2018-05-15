package tz.core.cluster.command;

import tz.core.cluster.state.Response;

import java.nio.ByteBuffer;

/**
 * Interface for command handlers, mostly for state machines
 */
public interface CommandExecutor
{
    /**
     * No OP command
     * @param cmd no op command
     */
    Response executeNoOPCommand(NoOPCommand cmd);

    /**
     * Register command, for client and node registration
     * @param cmd register command
     */
    Response executeRegisterCommand(RegisterCommand cmd);

    /**
     * Unregister command, for client and node registration
     * @param cmd unregister command
     */
    Response executeUnregisterCommand(UnregisterCommand cmd);

    /**
     * Config command, for cluster configuration
     * @param cmd config command
     */
    Response executeConfigCommand(ConfigCommand cmd);
}
