package tz.core.cluster;

import tz.base.common.Buffer;
import tz.base.exception.RaftException;
import tz.base.log.Log;
import tz.base.poll.Event;
import tz.base.record.ClusterRecord;
import tz.base.record.NodeRecord;
import tz.base.record.TransportRecord;
import tz.base.transport.listener.Listener;
import tz.core.Connection;
import tz.core.cluster.command.*;
import tz.core.cluster.request.*;
import tz.core.cluster.state.Response;
import tz.core.cluster.state.Session;
import tz.core.cluster.state.State;
import tz.core.msg.*;
import tz.core.worker.IOWorker.IOOwner;
import tz.core.worker.IOWorker.IOWorker;
import tz.core.worker.Worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.CRC32;

public class Cluster extends Worker implements RaftCluster, IOOwner
{
    public enum Role
    {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    private static final int ELECTION_TIMEOUT = 2000000;

    private final IOWorker ioWorker;
    private final Callbacks callbacks;
    private final Config config;
    private final State state;
    private final Store store;
    private final SnapshotReader snapshotReader;
    private final SnapshotWriter snapshotWriter;
    private final Path path;

    private final Map<String, Node> nodes;
    private final Map<String, Node> activeNodes;
    private final Map<String, Node> clients;
    private final Map<Long, Request> requests;
    private final List<Node> grantedVotes;
    private final List<Node> preVotes;
    private long preVoteTerm;

    private final List<Node> readyNodes;

    private NodeRecord nodeRecord;
    private ClusterRecord clusterRecord;
    private Role role;

    private long snapshotIndex;
    private long currentTerm;
    private long commit;
    private long matchIndex;
    private long onFlyIndex;
    private String votedFor;

    private Node leader;
    private Node own;

    private Buffer configBuf;
    private ElectionTimer electionTimer;
    private boolean termStarted;


    public Cluster(String clusterName, String nodeName, String workingDir,
                   Config config, Callbacks callbacks, State state) throws IOException
    {
        super(new Log(callbacks, config.logLevel), clusterName, false);

        this.ioWorker  = new IOWorker(this, log, clusterName + " IO Worker");
        this.callbacks = callbacks;
        this.config    = config;
        this.state     = state;


        nodeRecord     = new NodeRecord(nodeName, "");
        clusterRecord  = new ClusterRecord(clusterName);
        path           = Paths.get(workingDir + "/" + clusterName + "/" + nodeName + "/");
        own            = new Node(this, null, nodeRecord, nodeRecord, Node.Type.PEER);

        Files.createDirectories(path);

        snapshotReader = new SnapshotReader(this, path, clusterName, state);
        snapshotWriter = new SnapshotWriter(this, state, path, clusterName);
        store          = new Store(this, path, config.storeSize);

        role           = Role.FOLLOWER;
        configBuf      = new Buffer(ByteBuffer.allocateDirect(1024 * 1024));
        nodes          = new HashMap<>();
        activeNodes    = new HashMap<>();
        clients        = new HashMap<>();
        grantedVotes   = new ArrayList<>();
        requests       = new HashMap<>();
        readyNodes     = new ArrayList<>();
        preVotes       = new ArrayList<>();
        preVoteTerm    = -1;
        electionTimer  = new ElectionTimer(this, true,
                                           new Random().nextInt(150) + 2500,
                                           timestamp() + 500);
        addTimer(electionTimer);
        state.setCluster(this);

        nodes.put(own.getName(), own);

        open(clusterName, nodeName);
    }

    public void open(String clusterName, String nodeName) throws IOException
    {
        try {
            readMeta(clusterName, nodeName);
        }
        catch (Exception e) {
            logWarn("No config file at : ", path);
            snapshotReader.delete();
            store.deleteAll();

            // Go with initial config
            writeMeta();
            readMeta(clusterName, nodeName);
        }

        try {
            snapshotReader.readSnapshot();
        }
        catch (Exception e) {
            logInfo("Snapshot cannot be read at ", snapshotReader.getPath());
            snapshotReader.delete();

            // Go with initial state
            snapshotWriter.takeSnapshot();
            snapshotReader.readSnapshot();
        }

        commit = state.getIndex();
        store.open(commit);
    }

    private void readMeta(String clusterName, String nodeName) throws IOException
    {
        Path configPath    = Paths.get(this.path + "/" + "cluster" + ".conf");
        Path configTmpPath = Paths.get(this.path + "/" + "cluster" + ".conf.tmp");

        Path path = null;

        if (Files.exists(configPath)) {
            path = configPath;
        }
        else if (Files.exists(configTmpPath)) {
            path = configTmpPath;
        }
        else {
            throw new RaftException("No config file");
        }

        Set<StandardOpenOption> options = EnumSet.of(StandardOpenOption.READ);

        FileChannel channel = FileChannel.open(path, options);

        if (channel.size() > Integer.MAX_VALUE)  {
            channel.close();

            throw new RaftException("Config file size " +
                                        "is beyond the limit : " + channel.size());
        }

        if (channel.size() > configBuf.cap()) {
            configBuf = new Buffer(ByteBuffer.allocateDirect((int) channel.size()));
        }

        int read = 0;
        configBuf.clear();
        while (configBuf.hasRemaining() && read != -1) {
            read = channel.read(configBuf.backend());
        }
        configBuf.flip();

        final int len = configBuf.remaining() - Encoder.longLen(0);
        CRC32 crc32 = new CRC32();
        crc32.update(configBuf.slice(0, len).backend());

        nodeRecord    = new NodeRecord(configBuf);
        clusterRecord = new ClusterRecord(configBuf);
        currentTerm   = configBuf.getLong();
        votedFor      = configBuf.getString();

        long hash = configBuf.getLong();
        if (hash != crc32.getValue()) {
            throw new RaftException("Hash value of config file is inconsistent");
        }

        if (!clusterRecord.getName().equals(clusterName) ||
            !nodeRecord.getName().equals(nodeName)) {
            throw new IllegalArgumentException(
                "Read cluster : " + clusterRecord.getName() +
                "Read node    : " + nodeRecord.getName() + " does not match with : " +
                "Open cluster : " + clusterName +
                "Open node    : " + nodeName
            );
        }

        channel.close();
    }

    /**
     * Write metadata of the cluster to config file.
     */
    private void writeMeta()
    {
        Path configPath = Paths.get(this.path + "/" + "cluster" + ".conf");
        Path configTmpPath = Paths.get(this.path + "/" + "cluster" + ".conf.tmp");

        configBuf.clear();

        final int len = nodeRecord.rawLen() +
                        clusterRecord.rawLen() +
                        Encoder.longLen(currentTerm) +
                        Encoder.stringLen(votedFor) +
                        Encoder.longLen(0); // CRC32

        if (len > configBuf.cap()) {
            configBuf = new Buffer(ByteBuffer.allocateDirect(len));
        }

        nodeRecord.encode(configBuf);
        clusterRecord.encode(configBuf);
        configBuf.putLong(currentTerm);
        configBuf.putString(votedFor);

        CRC32 crc32 = new CRC32();
        crc32.update(configBuf.slice(0, configBuf.position()).backend());
        configBuf.putLong(crc32.getValue());
        configBuf.flip();

        try {
            Set<StandardOpenOption> options = EnumSet.of(StandardOpenOption.CREATE,
                                                         StandardOpenOption.READ,
                                                         StandardOpenOption.WRITE,
                                                         StandardOpenOption.SYNC,
                                                         StandardOpenOption.TRUNCATE_EXISTING);

            FileChannel tmp = FileChannel.open(configTmpPath, options);
            while (configBuf.hasRemaining()) {
                tmp.write(configBuf.backend());
            }

            tmp.close();

            Files.deleteIfExists(configPath);
            Files.move(configTmpPath, configPath);

        }
        catch (IOException e) {
            //Use unchecked exceptions for code clarity
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void join()
    {
        nodeRecord = clusterRecord.getRecord(nodeRecord.name);
        if (nodeRecord == null) {
            throw new RaftException("Local node record is not set");
        }

        if (!store.isStarted()) {
            writeMeta();
        }

        for (TransportRecord record : nodeRecord.transports) {
            ioWorker.addEndpoint(record);
        }

        for (NodeRecord record : clusterRecord.peers) {
            if (!record.isClient() && !record.equals(nodeRecord)) {
                Node node = new Node(this, null, nodeRecord, record, Node.Type.PEER);
                nodes.put(node.getName(), node);
                node.reconnect();
            }
        }

        ioWorker.start();
        start();
    }

    public IOWorker getIoWorker()
    {
        return ioWorker;
    }

    public String getClusterName()
    {
        return clusterRecord.name;
    }

    public State getState()
    {
        return state;
    }

    public Path getPath()
    {
        return path;
    }


    @Override
    public boolean isStarted()
    {
        return store.isStarted();
    }

    @Override
    public void addNode(NodeRecord nodeRecord)
    {
        clusterRecord.addNode(nodeRecord);
    }

    @Override
    public ClusterRecord getClusterRecord()
    {
        return null;
    }

    @Override
    public void sendListenerUpdate(Listener listener, boolean active)
    {

    }

    @Override
    public void handleListenerUpdate(Listener listener, boolean active)
    {

    }

    @Override
    public void sendConnectionUpdate(Connection conn, Connection.Status status)
    {
        addEvent(new ConnectionUpdate(this, conn, status));
    }

    public void handleReconnectTimer(Node node)
    {
        node.connect();
    }

    @Override
    public void handleConnectionUpdate(Connection conn, Connection.Status status)
    {
        Node node;

        switch (status)
        {
            case INCOMING:
                break;
            case OUTGOING_SUCCEED:
                node = (Node) conn.getAttachment();
                node.setConnected();
                node.sendConnectReq(clusterRecord.getName(),
                                    nodeRecord.getName(), false);
                break;

            case OUTGOING_FAILED:
                node = (Node) conn.getAttachment();
                node.reconnect();
                break;

            case DISCONNECTED:
                if (!conn.hasAttachment()) {
                    break;
                }

                node = (Node) conn.getAttachment();
                if (node.isClient()) {
                    clients.remove(node.getName());
                }
                else if (node.isPeer()) {
                    activeNodes.remove(node.getName());
                    node.reconnect();
                }
                break;
        }
    }

    @Override
    public void sendIncomingMsg(Connection conn, Msg msg)
    {
        addEvent(new IncomingMsg(this, conn, msg));
    }

    @Override
    public void handleIncomingMsg(Connection conn, Msg msg)
    {
        Node node = null;
        try {
            Object attachment = conn.getAttachment();
            if (attachment == null) {
                //This must be ConnectReq
                handleConnectReqMsg(conn, (ConnectReq) msg);
                return;
            }

            node = (Node) conn.getAttachment();
            node.addIncomingMsg(msg);
            readyNodes.add(node);
        }
        catch (Exception e) {
            logError(e);
            ioWorker.cancelConnection(conn);
        }
    }

    public void handleRequestCompleted(Node node, Entry entry,
                                       Response response)
    {
        node.sendClientResp(entry.getSequence(), response.success, response.data);
    }

    public void handleClientReq(Node node, ClientReq req)
    {
        Entry entry = new Entry(req.getStateId(), node.getId(),
                                req.getSequence(), req.getAcknowledge(),
                                currentTerm,  req.getData());
        store.add(entry);
        RequestCompleted comp = new RequestCompleted(node);
        comp.ioTs = req.ioTs;
        comp.clusterTs = req.clusterTs;

        requests.put(entry.getIndex(), comp);
    }

    public void handleConnectReqMsg(Connection conn, ConnectReq req)
    {
        if (req.isClient()) {
            if (role != Role.LEADER && !termStarted) {
                ioWorker.addOutgoingMsg(conn, new ConnectResp(false, clusterRecord, 0, 0));
                ioWorker.cancelConnection(conn);
                return;
            }

            Node node = clients.get(req.getName());
            if (node != null) {
                node.disconnect();
                node.setConnection(conn);
            }
            else {
                node = new Node(this, conn, nodeRecord,
                            new NodeRecord(req.getName(), ""), Node.Type.CLIENT);
                clients.put(req.getName(), node);
            }

            createInternalEntry(new RegisterCommand(req.getName()),
                                new ClientRegister(node));

        }
        else {
            if (!clusterRecord.name.equals(req.getClusterName())) {
                ioWorker.cancelConnection(conn);
                return;
            }

            Node node = nodes.get(req.getName());
            if (node != null && node.isDisconnected()) {
                node.setConnection(conn);
                activeNodes.put(req.getName(), node);
                node.sendConnectResp(true, clusterRecord, 0, 0);
            }
            else {
                ioWorker.cancelConnection(conn);
            }
        }
    }

    public void handleConnectRespMsg(Node node, ConnectResp connack)
    {
        if (connack.isSuccessful()) {
            activeNodes.put(node.getName(), node);
        }
    }

    public void handlePreVoteReq(Node node, PreVoteReq req)
    {
        boolean result = false;

        if (role == Role.LEADER ||
            leader != null ||
            req.getTerm() < currentTerm ||
            req.getLastLogIndex() < store.getLastIndex()) {
            result = false;
        }
        else if ((req.getTerm() > currentTerm)) {
            result = true;
        }

        node.sendPreVoteResp(currentTerm, store.getLastIndex(), result);
    }

    public void handlePreVoteResp(Node node, PreVoteResp req)
    {
        if (role != Role.CANDIDATE || preVoteTerm == -1) {
            logWarn("Unexpected message from ", node, " msg : ", req);
            return;
        }

        if (req.getTerm() > preVoteTerm) {
            currentTerm = req.getTerm();
            writeMeta();
            setRole(Role.FOLLOWER);
            preVoteTerm = -1;
            preVotes.clear();
            return;
        }

        if (req.isGranted()) {
            preVotes.add(node);
        }

        if (preVotes.size() >= nodes.size() / 2 + 1) {
            preVotes.clear();
            preVoteTerm = -1;
            grantedVotes.clear();

            votedFor = nodeRecord.name;
            currentTerm++;
            writeMeta();

            handleReqVoteResp(own, new ReqVoteResp(currentTerm,
                                                   store.getLastIndex(), true));

            for (Node active : activeNodes.values()) {
                active.sendReqVoteReq(currentTerm, store.getLastIndex(),
                                      store.getLastTerm(), false);
            }
        }
    }

    /**
     * Handle requestVoteMsg callback
     * @param node        message sender node
     * @param requestVote requestVoteMessage
     */
    public void handleReqVoteReq(Node node, ReqVoteReq requestVote)
    {
        boolean result = false;

        if (role == Role.LEADER ||
            leader != null ||
            requestVote.getTerm() < currentTerm ||
            requestVote.getLastLogIndex() < store.getLastIndex()) {
            result = false;
        }
        else if ((requestVote.getTerm() > currentTerm) ||
                 (votedFor == null                   ) ||
                 (votedFor.equals(node.getName())))
        {
            currentTerm = requestVote.getTerm();
            votedFor    = node.getName();

            writeMeta();

            setRole(Role.FOLLOWER);
            leader = node;

            result = true;
        }

        node.sendReqVoteResp(currentTerm, store.getLastIndex(), result);
    }

    /**
     * Handle requestVoteMsg callback
     * @param node            message sender node
     * @param requestVoteResp requestVoteMessage
     */
    public void handleReqVoteResp(Node node, ReqVoteResp requestVoteResp)
    {
        if (role != Role.CANDIDATE) {
            return;
        }

        if (requestVoteResp.getTerm() > currentTerm) {
            currentTerm = requestVoteResp.getTerm();
            writeMeta();
            setRole(Role.FOLLOWER);
            return;
        }

        if (requestVoteResp.isVoteGranted()) {
            grantedVotes.add(node);

            //Check if we got enough votes to become a leader
            if (grantedVotes.size() >= nodes.size() / 2 + 1) {
                setRole(Role.LEADER);
                for (Node follower : nodes.values()) {
                    follower.setNextIndex(store.getLastIndex() + 1);
                    follower.setMatchIndex(store.getLastIndex());
                }

                createInternalEntry(new NoOPCommand(), new TermStart());
            }
        }
    }

    /**
     * Handle AppendEntriesMsg callback, this message is sent by leader to
     * followers
     *
     * @param node leader node
     * @param req  AppendReq message
     */
    public void handleAppendReq(Node node, AppendReq req)
    {
        if (req.getTerm() < currentTerm) {
            node.sendAppendResp(store.getLastIndex(), currentTerm, false);
            return;
        }

        if (req.getTerm() > currentTerm) {
            setRole(Role.FOLLOWER);
            currentTerm = req.getTerm();
            writeMeta();
        }

        if (leader != node) {
            leader = node;
            setRole(Role.FOLLOWER);
        }

        Entry prev = store.get(req.getPrevLogIndex());
        long prevTerm = prev != null ? prev.getTerm() : snapshotReader.getTerm();
        if (prevTerm != req.getPrevLogTerm()) {
            node.sendAppendResp(store.getLastIndex(), currentTerm, false);
            return;
        }

        List<Entry> entries = req.getEntries();
        for (Entry entry : entries) {
            registerEntry(entry);
        }

        node.sendAppendResp(store.getLastIndex(), currentTerm, true);
        incrementCommit(req.getLeaderCommit());

    }

    /**
     * Handle handleAppendResp callback, this message is sent by followers to
     * the leader
     *
     * @param node follower node
     * @param resp resp message
     */
    public void handleAppendResp(Node node, AppendResp resp)
    {
        node.setMatchIndex(resp.getIndex());
        node.setNextIndex(resp.getIndex() + 1);
        if (resp.isSuccess()) {
            checkCommit(resp.getIndex());
        }
        else {
            if (resp.getTerm() > currentTerm) {
                currentTerm = resp.getTerm();
                writeMeta();
                setRole(Role.FOLLOWER);
            }
        }
    }

    public void handleInstallSnapshotReq(Node node, InstallSnapshotReq req)
    {
        System.out.println("dsad");
    }

    public void handleInstallSnapshotResp(Node node, InstallSnapshotResp resp)
    {

    }

    public void handleApplied(Entry entry, Response response)
    {
        Request req = requests.remove(entry.getIndex());
        if (req != null) {
            req.handle(this, entry, response);
        }
    }

    public void handleTermStart(Entry entry, Response response)
    {
        termStarted = true;

        logInfo("Term started : ", currentTerm, " Leader : ", nodeRecord,
                " Cluster : ", clusterRecord);

        for (NodeRecord peer : clusterRecord.peers) {
            if (peer.getName().equals(nodeRecord.getName())) {
                peer.setLeader();
                peer.setConnected(true);
            }
            else {
                peer.setPeer();
                if (activeNodes.get(peer.getName()) != null) {
                    peer.setConnected(true);
                }
                else {
                    peer.setConnected(false);
                }
            }
        }

        createInternalEntry(new ConfigCommand(clusterRecord),
                            new ConfigUpdate(clusterRecord));

    }

    public void handleClientRegisterCompleted(ClientRegister req, Entry entry,
                                              Response response)
    {
       Node node = req.getNode();
       if (response.isSuccess()) {
           Session session = state.getSessionData(node.getName());
           node.setId(session.getId());
           node.setSequence(session.getSequence());
           node.sendConnectResp(true, clusterRecord,
                                session.getSequence(), session.getAcknowledge());
       }
       else {
           node.sendConnectResp(false, clusterRecord, 0, 0);
       }
    }

    public void handlePeerRegisterCompleted(PeerRegister req, Entry entry,
                                            Response response)
    {

    }

    public void handleUnregisterCompleted(NodeRecord nodeRecord, ClusterRecord record,
                                          long sequence, boolean success)
    {
    }

    public void handleConfigUpdate(ClusterRecord record, Entry entry, Response response)
    {
        if (termStarted) {
            logInfo("Config updated : ", record);

           /*for (Node client : clients.values()) {
                client.sendPublishReq(record);
            }*/
        }
    }

    @Override
    public void handleEvents(Deque<Event> events)
    {
        //System.out.println("events : "+ events.size());
        try {
            Event event;
            while ((event = events.poll()) != null) {
                event.onEvent();
            }
        }
        catch (Exception e) {
            logError(e);
        }

        flush();
    }

    private void setRole(Role newRole)
    {
        switch (role) {
            case LEADER:
                switch (newRole) {
                    case LEADER:
                        break;
                    case CANDIDATE:
                        break;
                    case FOLLOWER:
                        //stopHeartBeatTimer();
                        addTimer(electionTimer);
                        break;
                }
                break;
            case CANDIDATE:
                switch (newRole) {
                    case LEADER:
                        removeTimer(electionTimer);
                        //startHeartBeatTimer();
                        break;
                    case CANDIDATE:
                        break;
                    case FOLLOWER:
                        //restartElectionTimer();
                        removeTimer(electionTimer);
                        addTimer(electionTimer);
                        break;
                }
                break;
            case FOLLOWER:
                switch (newRole) {
                    case LEADER:
                        removeTimer(electionTimer);
                        //startHeartBeatTimer();
                        break;
                    case CANDIDATE:
                        //restartElectionTimer();
                        removeTimer(electionTimer);
                        addTimer(electionTimer);
                        break;
                    case FOLLOWER:
                        //restartElectionTimer();
                        removeTimer(electionTimer);
                        addTimer(electionTimer);
                        break;
                }
        }

        role = newRole;

        logInfo("Became : ", role);
    }

    public Entry createInternalEntry(Command cmd, Request request)
    {
        Entry entry = new Entry(State.INTERNAL_ID, State.LEADER_ID,
                                State.LEADER_SEQ, State.LEADER_ACK,
                                currentTerm, cmd.getRaw());
        store.add(entry);
        requests.put(entry.getIndex(), request);

        return entry;
    }

    private void registerEntry(Entry entry)
    {
        Entry prev = store.get(entry.getIndex());
        if (prev != null) {
            if (prev.getTerm() != entry.getTerm()) {
                store.removeFrom(entry.getIndex());
            }
            return;
        }

        store.add(entry);
    }

    public void flush()
    {
        for (Node node : readyNodes) {
            node.handleMsgs();
        }

        readyNodes.clear();

        store.flush();

        if (role == Role.LEADER) {

            for (Node node : activeNodes.values()) {
                long nextIndex = node.getNextIndex();
                if (nextIndex > store.getLastIndex()) {
                    continue;
                }

                if (nextIndex <= snapshotReader.getIndex()) {
                    if (!node.hasSnapshotSender()) {
                        node.setSnapshotSender(new SnapshotSender(this,
                                                                  snapshotReader.getPath(),
                                                                  snapshotReader.getTerm(),
                                                                  snapshotReader.getIndex()));
                    }

                    if (!node.isSnapshotSendComplete()) {
                        node.sendInstallSnapshot(currentTerm);
                    }
                }
                else {

                    Entry prev = store.get(nextIndex - 1);
                    long prevTerm = prev != null ? prev.getTerm() :
                                                   snapshotReader.getTerm();
                    AppendReq req = new AppendReq(currentTerm, nextIndex - 1,
                                                  prevTerm, commit);

                    req.setEntriesBuffer(store.rawEntriesFrom(nextIndex));

                    node.setNextIndex(store.getLastIndex() + 1);
                    node.sendAppendReq(req);
                }
            }

            if (own.getNextIndex() <= store.getLastIndex()) {
                own.setMatchIndex(store.getLastIndex());
                own.setNextIndex(store.getLastIndex() + 1);
                handleAppendResp(own, new AppendResp(store.getLastIndex(),
                                                     currentTerm, true));
            }
        }

        checkCompaction();
    }

    private void incrementCommit(long index)
    {
        if (commit >= index) {
            return;
        }

        for (long i = commit + 1; i <= index; i++) {
            Entry entry = store.get(i);
            Response response = state.apply(entry);
            handleApplied(entry, response);
        }

        commit = index;
    }

    /**
     * Check if index is eligible to be marked committed
     *
     * @param index commit index
     */
    private void checkCommit(long index)
    {
        int count = 0;
        for (Node peer : activeNodes.values()) {
            if (peer.getMatchIndex() >= index) {
                count++;
            }
        }

        if (count == activeNodes.size()) {
            matchIndex = index;
        }

        if (count + 1 > (nodes.size() + 1) / 2) {
            if (commit >= index) {
                return;
            }

            for (long i = commit + 1; i <= index; i++) {
                Entry entry = store.get(i);
                Response response = state.apply(entry);
                handleApplied(entry, response);
            }

            commit = index;
        }
    }

    public void checkCompaction()
    {
        long index = store.getFirstPageEnd();
        if (commit > index) {
            snapshotWriter.takeSnapshot();
            store.deleteFirst();
        }
    }


    public void onElectionTimeout()
    {
        if (leader != null) {
            if (leader.getInTimestamp() + ELECTION_TIMEOUT > timestamp()) {
                return;
            }
        }

        if (activeNodes.size() + 1 >= (nodes.size() / 2) + 1) {
            if (preVoteTerm != -1 && role == Role.CANDIDATE) {
                logWarn("Couldnt get enough pre-votes from cluster " +
                        "in election timeout, trying again...");
            }

            preVotes.clear();
            setRole(Role.CANDIDATE);
            preVoteTerm = currentTerm + 1;

            handlePreVoteResp(own, new PreVoteResp(currentTerm,
                                                   store.getLastIndex(), true));

            for (Node node : activeNodes.values()) {
                node.sendPreVoteReq(preVoteTerm,
                                    store.getLastIndex(), store.getLastTerm());
            }
        }
    }
}
