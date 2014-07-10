/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.apache.openejb.server.cxf.rs;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.openejb.OpenEjbContainer;
import org.apache.openejb.config.DeploymentFilterable;
import org.apache.openejb.server.cxf.rs.beans.SimpleEJB;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.embeddable.EJBContainer;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Request;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EjbDeploymentTest {
    private static EJBContainer container;
    private static RESTIsCoolTwo service;

    @BeforeClass
    public static void start() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(DeploymentFilterable.CLASSPATH_INCLUDE, ".*openejb-cxf-rs.*");
        properties.setProperty(OpenEjbContainer.OPENEJB_EMBEDDED_REMOTABLE, "true");
        container = EJBContainer.createEJBContainer(properties);
        service = (RESTIsCoolTwo) container.getContext().lookup("java:/global/openejb-cxf-rs/RESTIsCoolTwo");
    }

    @AfterClass
    public static void close() throws Exception {
        if (container != null) {
            container.close();
        }
    }

    @Test
    public void normal() {
        assertNotNull(service);
        assertEquals("ok", service.normal());
    }

    @Test
    public void rest() {
        final String response = WebClient.create("http://localhost:4204/openejb-cxf-rs").path("/ejb/rest").get(String.class);
        assertEquals("ok", response);
    }

    @Test
    public void restParameterInjected() {
        String response = WebClient.create("http://localhost:4204/openejb-cxf-rs").path("/ejb/param").get(String.class);
        assertEquals("true", response);

        response = WebClient.create("http://localhost:4204/openejb-cxf-rs").path("/ejb/param").query("arg", "foo").get(String.class);
        assertEquals("foo", response);
    }

    @Test
    public void restFieldInjected() {
        final Boolean response = WebClient.create("http://localhost:4204/openejb-cxf-rs").path("/ejb/field").get(Boolean.class);
        assertEquals(true, response);
    }

    @Stateless
    @Path("/ejb")
    public static class RESTIsCoolTwo {
        @EJB
        private SimpleEJB simpleEJB;
        @javax.ws.rs.core.Context
        Request request;

        @Path("/normal")
        @GET
        public String normal() {
            return simpleEJB.ok();
        }

        @Path("/rest")
        @GET
        public String rest() {
            return simpleEJB.ok();
        }

        @Path("/param")
        @GET
        public String param(@QueryParam("arg") @DefaultValue("true") final String p) {
            return p;
        }

        @Path("/field")
        @GET
        public boolean field() {
            return "GET".equals(request.getMethod());
        }
    }
}
