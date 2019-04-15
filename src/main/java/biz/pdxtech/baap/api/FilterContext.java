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
package biz.pdxtech.baap.api;

import biz.pdxtech.baap.filter.ChaincodeContext;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FilterContext implements IFilter {
    
    int code2chainIdx = 0;
    int chain2codeIdx = 0;
    
    List<IFilter> filters = Collections.synchronizedList(new LinkedList<>());
    
    /**
     * global context, chaincodeID or BAAP_CTX_ID
     */
    Map<String, ChaincodeContext> context = null;
    
    /**
     * Constructor
     * 
     * @param filters
     * 
     * @param context
     *            GLOBAL framework context
     */
    public FilterContext(List<IFilter> filters, Map<String, ChaincodeContext> context) {
        this.filters = filters;
        this.context = context;
    }
    
    public void setChaincodeContext(ChaincodeContext cc) {
        this.context.put(cc.getChaincodeID().getName(), cc);
    }
    
    public ChaincodeContext getChaincodeContext(String chaincodeName) {
        return this.context.get(chaincodeName);
    }
    
    /**
     * Manipulating a <b>chaincode --> blockchain </b> message on the fly
     * 
     * @param msg
     *            msg to manipulate on the fly
     * @param chain
     *            next filter in the chaincode filter chain
     * @return manipulated msg. null if black-holed (beware consequences)
     */
    public ChaincodeMessage code2chain(ChaincodeMessage msg, FilterContext chain) {
        if (msg == null)
            return null;
        code2chainIdx++;
        if (code2chainIdx == filters.size())
            return msg;
        else
            return filters.get(code2chainIdx).code2chain(msg, chain);
    }
    
    /**
     * Manipulating a <b> blockchain --> chaincode </b> message on the fly
     * 
     * @param msg
     *            msg to manipulate on the fly
     * @param chain
     *            next filter in the chaincode filter chain
     * @return manipulated msg. null if block-holed (beware consequences)
     */
    public ChaincodeMessage chain2code(ChaincodeMessage msg, FilterContext chain) {
        if (msg == null)
            return null;
        chain2codeIdx++;
        if (chain2codeIdx == filters.size())
            return msg;
        else
            return filters.get(chain2codeIdx).chain2code(msg, chain);
    }
}
