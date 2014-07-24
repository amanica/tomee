/**
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

import org.apache.openejb.cdi.CdiAppContextsService;
import org.apache.openejb.cdi.ThreadSingletonServiceImpl;
import org.apache.openejb.cdi.WebappWebBeansContext;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.FailOverService;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 *          <p/>
 *          Used as a stack executed at the end of the request too. Avoid multiple (useless) listeners.
 */
public class EndWebBeansListener implements ServletContextListener, ServletRequestListener, HttpSessionListener, HttpSessionActivationListener {

    static final ThreadLocal<Boolean> FAKE_REQUEST = new ThreadLocal<Boolean>();

    private final String contextKey;

    /**
     * Logger instance
     */
    private static final Logger logger = Logger.getInstance(LogCategory.OPENEJB_CDI, EndWebBeansListener.class);

    protected FailOverService failoverService;

    /**
     * Manages the container lifecycle
     */
    protected WebBeansContext webBeansContext;
    private final CdiAppContextsService contextsService;

    /**
     * Default constructor
     *
     * @param webBeansContext the OWB context
     */
    public EndWebBeansListener(final WebBeansContext webBeansContext) {
        this.webBeansContext = webBeansContext;
        if (webBeansContext != null) {
            this.failoverService = this.webBeansContext.getService(FailOverService.class);
            this.contextsService = CdiAppContextsService.class.cast(webBeansContext.getService(ContextsService.class));
            this.contextKey = "org.apache.tomee.catalina.WebBeansListener@" + webBeansContext.hashCode();
        } else {
            this.contextKey = "notused";
            this.contextsService = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestDestroyed(final ServletRequestEvent event) {
        if (webBeansContext == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Destroying a request : [{0}]", event == null ? "null" : event.getServletRequest().getRemoteAddr());
        }

        final Object oldContext;
        if (event != null) {
            oldContext = event.getServletRequest().getAttribute(contextKey);
        } else {
            oldContext = null;
        }

        try {
            if (event != null
                && failoverService != null
                && failoverService.isSupportFailOver()) {
                final Object request = event.getServletRequest();
                if (request instanceof HttpServletRequest) {
                    final HttpServletRequest httpRequest = (HttpServletRequest) request;
                    final javax.servlet.http.HttpSession session = httpRequest.getSession(false);
                    if (session != null) {
                        failoverService.sessionIsIdle(session);
                    }
                }
            }

            // clean up the EL caches after each request
            final ELContextStore elStore = ELContextStore.getInstance(false);
            if (elStore != null) {
                elStore.destroyELContextStore();
            }

            webBeansContext.getContextsService().endContext(RequestScoped.class, event);
            if (webBeansContext instanceof WebappWebBeansContext) { // end after child
                ((WebappWebBeansContext) webBeansContext).getParent().getContextsService().endContext(RequestScoped.class, event);
            }
        } finally {
            contextsService.removeThreadLocals();
            ThreadSingletonServiceImpl.enter((WebBeansContext) oldContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestInitialized(final ServletRequestEvent event) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionCreated(final HttpSessionEvent event) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionDestroyed(final HttpSessionEvent event) {
        if (webBeansContext == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Destroying a session with session id : [{0}]", event.getSession().getId());
        }

        // ensure session ThreadLocal is set
        webBeansContext.getContextsService().startContext(SessionScoped.class, event.getSession());

        webBeansContext.getContextsService().endContext(SessionScoped.class, event.getSession());
        if (WebappWebBeansContext.class.isInstance(webBeansContext)) { // end after child
            WebappWebBeansContext.class.cast(webBeansContext).getParent().getContextsService().endContext(SessionScoped.class, event.getSession());
        }

        final ConversationManager conversationManager = webBeansContext.getConversationManager();
        final Map<Conversation, ConversationContext> cc = conversationManager.getAndRemoveConversationMapWithSessionId(event.getSession().getId());
        for (final ConversationContext c : cc.values()) {
            if (c != null) {
                c.destroy();
            }
        }

        destroyFakedRequest();
    }

    private void destroyFakedRequest() {
        final Boolean faked = FAKE_REQUEST.get();
        try {
            if (faked != null && faked) {
                requestDestroyed(null);
            }
        } finally {
            FAKE_REQUEST.remove();
        }
    }


    @Override
    public void sessionWillPassivate(final HttpSessionEvent event) {
        if (webBeansContext == null) {
            return;
        }

        if (failoverService != null && failoverService.isSupportPassivation()) {
            failoverService.sessionWillPassivate(event.getSession());
        }
        destroyFakedRequest();
    }

    @Override
    public void sessionDidActivate(final HttpSessionEvent event) {
        // no-op
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        destroyFakedRequest();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        destroyFakedRequest();
    }
}
