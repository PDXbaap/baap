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
import biz.pdxtech.baap.api.IFilter;
import biz.pdxtech.baap.util.json.BaapJSONUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterFramework {

    private static final Logger logger = LoggerFactory.getLogger(FilterFramework.class);

    @Getter
    @Setter
    @Nonnull
    public static class FilterInfo {
        String name;
        String ver;
        String info;
        Map<String, String> conf = new HashMap<>();

        public FilterInfo() {}
    }

    @Nonnull
    public static class FilterChainConfig extends LinkedList<FilterInfo> {
        private static final long serialVersionUID = -4795768214033284624L;

        public FilterChainConfig() {
        }
    }

    private static FilterFramework instance = new FilterFramework();

    private boolean filtering = true;

    public static final String BAAP_CTX_ID = "61cca86800ca29c22c7c43eda68e4a8e"; // MD5(PDXbaap)

    // chaincodeID or BAAP_CTX_ID
    private Map<String, ChaincodeContext> context = new ConcurrentHashMap<>();

    private List<IFilter> filters = new LinkedList<>();

    public static FilterFramework getInstance() {
        if (instance == null) {
            synchronized (FilterFramework.class) {
                if (instance == null)
                    instance = new FilterFramework();
            }
        }
           return instance;
    }

    private FilterFramework() {

        String flag = System.getenv("BAAP_GRPC_CC_FILTERING");

        if (flag == null) {
            flag = System.getProperty("BAAP_GRPC_CC_FILTERING", "true");
        }

        this.filtering = Boolean.parseBoolean(flag);

        if (this.filtering) {
            this.loadFilters();
        }
    }

    public void loadFilters() {

        try {

            Map<String, Map<String, String>> name2conf = new HashMap<>();

            FilterChainConfig confList = BaapJSONUtil.fromJson(
                    IOUtils.toString(FilterFramework.class.getResourceAsStream("/META-INF/chaincode-filter-list.conf"), "utf-8"),
                    FilterChainConfig.class);

            for (FilterInfo fi : confList) {
                name2conf.put(fi.name, fi.conf);
            }

            Map<String, IFilter> name2object = new HashMap<>();

            ServiceLoader<IFilter> loader = ServiceLoader.load(IFilter.class);
            for (IFilter f : loader) {
                Map<String, String> c = name2conf.get(f.getClass().getCanonicalName());
                f.init(c);
                name2object.put(f.getClass().getCanonicalName(), f);
            }

            for (FilterInfo fi : confList) {
                filters.add(name2object.get(fi.name));
                logger.info("chaincode filter: < {} {} {} loaded", fi.name, fi.ver, fi.info);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ChaincodeMessage code2chain(ChaincodeMessage msg) {

        if (!this.filtering || this.filters.size() == 0) {
            return msg;
        }

        return this.filters.get(0).code2chain(msg, new FilterContext(this.filters, this.context));
    }

    public ChaincodeMessage chain2code(ChaincodeMessage msg) {

        if (!this.filtering || this.filters.size() == 0) {
            return msg;
        }

        return this.filters.get(0).chain2code(msg, new FilterContext(this.filters, this.context));
    }
}
