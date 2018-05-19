package tz.core.cluster.state;

import tz.base.common.Buffer;
import tz.base.common.Tuple;
import tz.base.common.Util;
import tz.base.record.ClusterRecord;
import tz.base.record.NodeRecord;
import tz.core.cluster.Cluster;
import tz.core.cluster.command.*;
import tz.core.msg.Encoder;
import tz.core.msg.Entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract state class, application states must extend this
 */
public abstract class State implements CommandExecutor
{
    public static final int INTERNAL_ID = 0;
    public static final int USER_ID     = 1;
    public static final int LEADER_ID   = 0;
    public static final int LEADER_SEQ  = 0;
    public static final int LEADER_ACK  = 0;

    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
    private static final Response EMPTY_SUCCESS = new Response(0, true, EMPTY_BUF);

    private long index;
    private long term;

    private Map<Integer, Session> sessions;
    private ClusterRecord record;

    private Cluster cluster;

    /**
     * Create a new State
     */
    public State()
    {
        sessions = new HashMap<>();
        record   = new ClusterRecord("");

        sessions.put(State.LEADER_ID, new Session("", State.LEADER_ID));
    }

    public Session getSessionData(String name)
    {
        for (Session session : sessions.values()) {
            if (session.name.equals(name)) {
                return session;
            }
        }

        return null;
    }

    public ClusterRecord getRecord()
    {
        return record;
    }

    public void setCluster(Cluster cluster)
    {
        this.cluster = cluster;
    }

    /**
     * Set latest log term for the state
     * @param term term
     */
    public void setTerm(long term)
    {
        this.term = term;
    }

    /**
     * Get latest log term
     * @return term
     */
    public long getTerm()
    {
        return term;
    }

    /**
     * Get latest log index
     * @return latest log index
     */
    public long getIndex()
    {
        return index;
    }

    /**
     * Set latest log index
     * @param index latest log index
     */
    public void setIndex(long index)
    {
        this.index = index;
    }

    public abstract void clear();

    /**
     * Save(serialize) this state to outputstream)
     * @param out          outputstream
     * @throws IOException on any IO error
     */
    public abstract void saveState(OutputStream out) throws IOException;

    /**
     * Load(deserialize) this state from inputstream)
     * @param in           inputstream
     * @throws IOException on any IO error
     */
    public abstract void loadState(InputStream in) throws IOException;

    /**
     * Process command
     * @param buf raw encoded command
     */
    public abstract ByteBuffer onCommand(long index, ByteBuffer buf);
    /**
     * On command received
     */
    public Response apply(Entry entry)
    {
        Session session = sessions.get(entry.getClientId());
        Response response = session.getResponse(entry.getSequence());
        if (response == null) {
            switch (entry.getStateId()) {
                case State.INTERNAL_ID:
                    Command cmd = Command.create(entry.getData());
                    response = cmd.execute(this);
                    break;
                case State.USER_ID:
                    ByteBuffer data = onCommand(entry.getIndex(), entry.getCommand());
                    response = new Response(entry.getSequence(), true, data);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown state id");
            }

            session.cache(entry, response);

            index = entry.getIndex();
            term  = entry.getTerm();
        }

        return response;
    }


    public void save(OutputStream out) throws IOException
    {
        int len = Encoder.longLen(term) +
                  Encoder.longLen(index) +
                  record.rawLen() +
                  Encoder.intLen(sessions.size());

        Buffer buf = new Buffer(len + Encoder.intLen(len));

        buf.putInt(len);
        buf.putLong(term);
        buf.putLong(index);
        record.encode(buf);
        buf.putInt(sessions.size());
        buf.flip();

        out.write(buf.array());

        for (Session session : sessions.values()) {
            session.encode(out);
        }

        saveState(out);
    }

    /**
     * Deserialize state from an inputstream
     *
     * @param in             inputstream
     * @throws IOException   on any IO error
     */

    public void load(InputStream in) throws IOException
    {
        int len = Util.readInt(in);

        Buffer buf = new Buffer(len);

        int read = in.read(buf.array(), 0, len);
        if (read != len) {
            throw new IOException("Snapshot is corrupt");
        }

        buf.position(len);
        buf.flip();

        term   = buf.getLong();
        index  = buf.getLong();
        record = new ClusterRecord(buf);

        int sessionCount = buf.getInt();
        for (int i = 0; i < sessionCount; i++) {
            Session session = new Session(in);
            sessions.put(session.id, session);
        }

        loadState(in);
    }

    /**
     * Handle no op command
     * @param cmd no op command
     */
    @Override
    public Response executeNoOPCommand(NoOPCommand cmd)
    {
        return EMPTY_SUCCESS;
    }

    /**
     * Register command, create session for required id, if exists, return
     * existing record
     *
     * @param cmd register command
     */
    @Override
    public Response executeRegisterCommand(RegisterCommand cmd)
    {
        for (Session session : sessions.values()) {
            if (session.name.equals(cmd.getName())) {
                return EMPTY_SUCCESS;
            }
        }

        for (int i = 0; i < sessions.size() + 1; i++) {
            boolean found = false;
            for (Session existing : sessions.values()) {
                if (existing.id == i) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                sessions.put(i, new Session(cmd.getName(), i));
                return EMPTY_SUCCESS;
            }
        }

        throw new IllegalStateException("Cannot register client : " + cmd.getName());
    }

    /**
     * Unregister command callback
     * @param cmd unregister command
     */
    @Override
    public Response executeUnregisterCommand(UnregisterCommand cmd)
    {
        /*NodeRecord nodeRecord = null;
        Session session = idToSession.remove(cmd.getId());
        if (session != null) {
            sessions.remove(session.getName());
            nodeRecord = record.removeRecord(session.getName());
        }

        cluster.handleUnregisterCompleted(nodeRecord, record, sequence);*/
        return EMPTY_SUCCESS;
    }

    /**
     * Config command callback
     * @param cmd config command
     */
    @Override
    public Response executeConfigCommand(ConfigCommand cmd)
    {
        record = cmd.getRecord();

        /*
        for (NodeRecord node : record.peers) {
            Session session = sessions.get(node.getName());
            if (session == null) {
                createSession(node.getName(), node.getId(), 0);
            }
        }

        for (NodeRecord node : record.clients) {
            if (sessions.get(node.getName()) == null) {
                createSession(node.getName(), node.getId(), 0);
            }
        }*/

        return EMPTY_SUCCESS;
    }
}
