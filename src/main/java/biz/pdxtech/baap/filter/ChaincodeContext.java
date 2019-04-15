/*************************************************************************
 * Copyright (C) 2016-2019 The PDX Blockchain Hypercloud Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *************************************************************************/
package biz.pdxtech.baap.filter;

import biz.pdxtech.baap.api.FilterContext;
import org.hyperledger.fabric.protos.peer.Chaincode.*;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChaincodeContext {
    
    public static enum State {
        CREATED, // not yet connected
        ESTABLISHED, // on connection established
        READY,	// triggered by successful register
    };
    
    State state;
    
    ChaincodeID chaincodeID;
    
    String txid; // set on initiation, reset on complete/response
    
    ChaincodeMessage req; // set on request proxing, reset on complete/response/error
    
    /**
     * chaincode context. key schema: txid://{txid-value} - remember to remove when
     * not needed
     */
    Map<String, Object> context = new ConcurrentHashMap<>();
    
    private FilterContext filterContext;
    
    public ChaincodeContext(FilterContext filterContext, ChaincodeID chaincodeID) {
        this.filterContext = filterContext;
        this.chaincodeID = chaincodeID;
        this.filterContext.setChaincodeContext(this);
    }
    
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public ChaincodeID getChaincodeID() {
        return chaincodeID;
    }
    
    public String getTxid() {
        return txid;
    }
    
    public void setTxid(String txid) {
        this.txid = txid;
    }
    
    public ChaincodeMessage getReq() {
        return req;
    }
    
    public void setReq(ChaincodeMessage req) {
        this.req = req;
    }
    
    /**
     * Set chaincode context for bi-directional correlation if needed.
     * 
     * Note that BaaP-populated context (of type Map<String, Object>) is under
     * biz.pdxtech.baap.ccfilter.FilterContext key
     * 
     * @param key
     *            fully qualified class name of the filter.
     * @param value
     *            value of the filter-specific context
     */
    public void setContext(String key, Object value) {
        this.context.put(key, value);
    }
    
    /**
     * Get pchaincode context for bi-directional correlation if needed.
     * 
     * Note that BaaP-populated context (of type Map<String, Object>) is under
     * biz.pdxtech.baap.ccfilter.FilterContext key
     *
     * @param key
     *            fully qualified class name of the filter.
     * @return context object
     */
    public Object getContext(String key) {
        return this.context.get(key);
    }
}
