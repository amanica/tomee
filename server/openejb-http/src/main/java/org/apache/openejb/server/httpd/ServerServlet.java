/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.server.httpd;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.server.ServiceException;
import org.apache.openejb.server.context.RequestInfos;
import org.apache.openejb.server.ejbd.EjbServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServerServlet extends HttpServlet {
    private static final String ACTIVATED_INIT_PARAM = "activated";

    private EjbServer ejbServer;
    private boolean activated = SystemInstance.get().isDefaultProfile();

    public void init(final ServletConfig config) {
        ejbServer = SystemInstance.get().getComponent(EjbServer.class);
        final String activatedStr = config.getInitParameter(ACTIVATED_INIT_PARAM);
        if (activatedStr != null) {
            activated = Boolean.getBoolean(ACTIVATED_INIT_PARAM);
        }
    }

    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (!activated) {
            response.getWriter().write("");
            return;
        }

        final ServletInputStream in = request.getInputStream();
        final ServletOutputStream out = response.getOutputStream();
        try {
            RequestInfos.initRequestInfo(request);
            ejbServer.service(in, out);
        } catch (final ServiceException e) {
            throw new ServletException("ServerService error: " + ejbServer.getClass().getName() + " -- " + e.getMessage(), e);
        } finally {
            RequestInfos.clearRequestInfo();
        }
    }
}
