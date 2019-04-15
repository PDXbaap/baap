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
package biz.pdxtech.baap.command;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

public class HttpClientUtil {

    private  Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private  Gson gson =new Gson();
    private  CloseableHttpClient closeableHttpClient;
    public HttpClientUtil(){
        Integer REQ_TIMEOUT =  60000;     //请求超时时间ms
        Integer CONN_TIMEOUT = 60000;     //连接超时时间ms
        Integer SOCK_TIMEOUT = 60000;    //读取超时时间ms
        HttpRequestRetryHandler requestRetryHandler=new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                boolean flag=false;
                if (executionCount > 3) //超过重试次数，就放弃
                    return  false;
                if (exception instanceof NoHttpResponseException) {//没有响应，重试
                    return true;
                }else if (exception instanceof ConnectTimeoutException) {//连接超时，重试
                    return true;
                } else if (exception instanceof SocketTimeoutException) {//连接或读取超时，重试
                    return true;
                }else if (exception instanceof SSLHandshakeException) {//本地证书异常
                    flag= true;
                } else if (exception instanceof InterruptedIOException) {//被中断
                    flag= true;
                } else if (exception instanceof UnknownHostException) {//找不到服务器
                    flag= true;
                }  else if (exception instanceof SSLException) {//SSL异常
                    flag= true;
                } else {
                    logger.error("Unrecorded request exception >>>>>：" + exception.getClass());
                }
                if(flag){
                    logger.error("Failed interface call >>>>>：" + exception.getClass());
                    return  false;
                }
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，则重试
                if (!(request instanceof HttpEntityEnclosingRequest)) return true;
                return false;
            }
        };
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50000);
        cm.setDefaultMaxPerRoute(500);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(REQ_TIMEOUT)
                .setConnectTimeout(CONN_TIMEOUT).setSocketTimeout(SOCK_TIMEOUT)
                .build();
     this.closeableHttpClient= HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(requestConfig).setRetryHandler(requestRetryHandler).build();

    }

    public  String doGet(String url, Map<String, String> param) {

        // 创建Httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String resultString = "";
        CloseableHttpResponse response = null;
        try {
            // 创建uri
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();

            // 创建http GET请求
            HttpGet httpGet = new HttpGet(uri);

            // 执行请求
            response = this.closeableHttpClient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
            logger.info("Httprequest info result:{}",resultString);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Httprequest error stack:{}",e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultString;
    }

    public  String doGet(String url) {
        return doGet(url, null);
    }

    public  String doPost(String url, Object params) {
        String json=gson.toJson(params);
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url);
            // 创建参数列表
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            // 执行http请求
            // 执行http请求
            response = this.closeableHttpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
            logger.info("Httprequest info result:{}",resultString);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Httprequest error stack:{}",e);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return resultString;
    }

}