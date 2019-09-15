package com.keepgulp.megaapi.client;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class MegaDirNode {

    private final String _node_id;

    private final HashMap<String, MegaDirNode> _children;

    public MegaDirNode(String node_id) {

        _node_id = node_id;

        _children = new HashMap<>();
    }

    public String getNode_id() {
        return _node_id;
    }

    public HashMap<String, MegaDirNode> getChildren() {
        return _children;
    }
    private static final Logger LOG = Logger.getLogger(MegaDirNode.class.getName());

}
