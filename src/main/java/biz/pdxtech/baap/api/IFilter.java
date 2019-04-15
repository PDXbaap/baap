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


import org.hyperledger.fabric.protos.peer.ChaincodeShim.*;
import java.util.Map;


public interface IFilter {
    
    /**
     * Initialize chaincode filter.
     * 
     */
    public default void init(Map<String, String> conf) {
    }
    
    /**
     * Destroy chaincode filter. Calling not guaranteed.
     */
    public default void destroy() {
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
    public default ChaincodeMessage code2chain(ChaincodeMessage msg, FilterContext chain) {
        return chain.code2chain(msg, chain);
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
    public default ChaincodeMessage chain2code(ChaincodeMessage msg, FilterContext chain) {
        return chain.chain2code(msg, chain);
    }
}
