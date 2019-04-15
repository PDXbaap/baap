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
package biz.pdxtech.baap.chain;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EngineConf {
	
	String  id;
	String  type;
	boolean tls;
	String  host;
	int     port;
	int     rpcport;
	boolean connect;
	String enode;
	
	public EngineConf() {}
	
	public EngineConf(String id, String type, String ver, boolean tls, String host, int port, int rpcport,String enode) {
		super();
		this.id = id;
		this.type = type;
		this.tls = tls;
		this.host = host;
		this.port = port;
		this.rpcport = rpcport;
		this.enode=enode;
	}
	
	public enum EngineType {
		FABRIC("fabric"),
		ETHEREUM("ethereum"),
		PDX("pdx");
		private String name;
		
		EngineType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
	}

}
