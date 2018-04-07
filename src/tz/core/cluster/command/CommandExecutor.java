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
     *
     * @param cmd No op command
     * @return    Command response
     */
    Response executeNoOPCommand(NoOPCommand cmd);

    /**
     * Register command, for client and node registration
     *
     * @param cmd register command
     * @return    Command response
     */
    Response executeRegisterCommand(RegisterCommand cmd);

    /**
     * Unregister command, for client and node registration
     * @param cmd unregister command
     * @return    Command response
     */
    Response executeUnregisterCommand(UnregisterCommand cmd);

    /**
     * Config command, for cluster configuration
     * @param cmd config command
     * @return    Command response
     */
    Response executeConfigCommand(ConfigCommand cmd);
}
