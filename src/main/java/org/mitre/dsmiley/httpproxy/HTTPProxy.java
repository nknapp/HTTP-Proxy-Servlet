package org.mitre.dsmiley.httpproxy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import org.apache.http.message.HeaderGroup;

/**
 * A proxy interface for http requests.
 *
 * @author gpaul
 *         Created on 4/26/13 10:02 AM
 */
public interface HTTPProxy {

    /**
     * Release any resource held by this proxy.
     */
    void shutdown();

    /**
     * Proxy the given request
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws IOException
     * @throws ServletException
     */
    void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException;

    void setHeaderModificatons(HeaderModificatons headerModificatons);

    /**
     * Interface that provides an extension point to replace headers within the proxy request.
     * Instances of this class can be set via {@link #setHeaderModificatons(org.mitre.dsmiley.httpproxy.Proxy.HeaderModificatons)}
     * Instances of this class must be state less and my be used for multiple HTTPProxy instances simultaneously.
     */
    public static interface HeaderModificatons {

        /**
         * Returns a number of header-nams that should be stripped from the proxy request.
         * All header-names must be lower-case
         *
         * @return
         */
        HeaderGroup removals();

        /**
         * Returns a number of headers that should be additionally provided to the proxy request.
         * Replacements can be specified by providing a removal and an addition for the same header.
         * @return
         */
        HeaderGroup additions();

    }
}
