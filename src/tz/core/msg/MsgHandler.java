package tz.core.msg;

/**
 * Message handler callbacks
 */
public interface MsgHandler
{
    /**
     * Handle ConnectReq message
     * @param msg ConnectReq message
     */
    default void handleConnectReq(ConnectReq msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle ConnectResp message
     * @param msg ConnectResp message
     */
    default void handleConnectResp(ConnectResp msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle JoinReq message
     * @param msg JoinReq message
     */
    default void handleJoinReq(JoinReq msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle JoinResp message
     * @param msg JoinResp message
     */
    default void handleJoinResp(JoinResp msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle AppendReq message
     * @param msg AppendReq message
     */
    default void handleAppendReq(AppendReq msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle AppendResp message
     * @param msg AppendResp message
     */
    default void handleAppendResp(AppendResp msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle ReqVoteReq message
     * @param msg ReqVoteReq message
     */
    default void handleReqVoteReq(ReqVoteReq msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle ReqVoteResp message
     * @param msg ReqVoteResp message
     */
    default void handleReqVoteResp(ReqVoteResp msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle ClientReq message
     * @param msg ClientReq message
     */
    default void handleClientReq(ClientReq msg)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle ClientResp message
     * @param msg ClientResp message
     */
    default void handleClientResp(ClientResp msg)
    {
        throw new UnsupportedOperationException();
    }

    default void handlePublishReq(PublishReq msg)
    {
        throw new UnsupportedOperationException();
    }

    default void handlePreVoteReq(PreVoteReq msg)
    {
        throw new UnsupportedOperationException();
    }

    default void handlePreVoteResp(PreVoteResp msg)
    {
        throw new UnsupportedOperationException();
    }
}
