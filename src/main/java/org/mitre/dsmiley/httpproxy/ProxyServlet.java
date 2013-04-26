package org.mitre.dsmiley.httpproxy;

/**
 * Copyright MITRE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * An HTTP reverse proxy/gateway servlet. It is designed to be extended for customization
 * if desired. Most of the work is handled by
 * <a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpClient</a>.
 * <p>
 * There are alternatives to a servlet based proxy such as Apache mod_proxy if that is available to you. However
 * this servlet is easily customizable by Java, secure-able by your web application's security (e.g. spring-security),
 * portable across servlet engines, and is embeddable into another web application.
 * </p>
 * <p>
 * Inspiration: http://httpd.apache.org/docs/2.0/mod/mod_proxy.html
 * </p>
 *
 * @author David Smiley dsmiley@mitre.org>
 */
public class ProxyServlet extends HttpServlet {

  /* INIT PARAMETER NAME CONSTANTS */

    /**
     * A boolean parameter then when enabled will log input and target URLs to the servlet log.
     */
    public static final String P_LOG = "log";

  /* MISC */

    protected boolean doLog = false;
    protected URI targetUri;
    private HTTPProxy proxy;

    @Override
    public String getServletInfo() {
        return "A proxy servlet by David Smiley, dsmiley@mitre.org";
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        String doLogStr = servletConfig.getInitParameter(P_LOG);
        if (doLogStr != null) {
            this.doLog = Boolean.parseBoolean(doLogStr);
        }

        try {
            targetUri = new URI(servletConfig.getInitParameter("targetUri"));
        } catch (Exception e) {
            throw new RuntimeException("Trying to process targetUri init parameter: " + e, e);
        }
        HttpParams hcParams = new BasicHttpParams();
        readConfigParam(hcParams, ClientPNames.HANDLE_REDIRECTS, Boolean.class);
        proxy = new Proxy(targetUri, createHttpClient(hcParams), doLog);

    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        proxy.service(servletRequest, servletResponse);
    }

    /**
     * Called from {@link #init(javax.servlet.ServletConfig)}. HttpClient offers many opportunities for customization.
     *
     * @param hcParams
     */
    protected HttpClient createHttpClient(HttpParams hcParams) {
        return new DefaultHttpClient(new ThreadSafeClientConnManager(), hcParams);
    }
    /**
     *
     * @param hcParams
     * @param hcParamName
     * @param type
     */
    private void readConfigParam(HttpParams hcParams, String hcParamName, Class type) {
        String val_str = getServletConfig().getInitParameter(hcParamName);
        if (val_str == null)
            return;
        Object val_obj;
        if (type == String.class) {
            val_obj = val_str;
        } else {
            try {
                val_obj = type.getMethod("valueOf", String.class).invoke(type, val_str);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        hcParams.setParameter(hcParamName, val_obj);
    }

    @Override
    public void destroy() {
        if (proxy != null) {
            proxy.shutdown();
        }
        super.destroy();
    }

}
