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
package org.apache.openejb.core.stateless;

import junit.framework.TestCase;
import org.apache.openejb.OpenEJB;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.ProxyFactoryInfo;
import org.apache.openejb.assembler.classic.SecurityServiceInfo;
import org.apache.openejb.assembler.classic.StatelessSessionContainerInfo;
import org.apache.openejb.assembler.classic.TransactionServiceInfo;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.core.ivm.naming.InitContextFactory;
import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.jee.StatelessBean;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @version $Revision$ $Date$
 */
public class StatelessInstanceManagerPoolingTest extends TestCase {

    public static final AtomicInteger instances = new AtomicInteger();
    public static final AtomicInteger discardedInstances = new AtomicInteger();

    public void testStatelessBeanPooling() throws Exception {

        final InitialContext ctx = new InitialContext();
        final Object object = ctx.lookup("CounterBeanLocal");
        assertTrue("instanceof counter", object instanceof Counter);

        final CountDownLatch startPistol = new CountDownLatch(1);
        final CountDownLatch startingLine = new CountDownLatch(10);

        final Counter counter = (Counter) object;
        // Do a business method...
        final Runnable r = new Runnable() {
            public void run() {
                counter.race(startingLine, startPistol);
            }
        };

        //  -- READY --

        // How much ever the no of client invocations the count should be 10 as only 10 instances will be created.
        final Collection<Thread> th = new ArrayList<>(30);
        for (int i = 0; i < 30; i++) {
            final Thread t = new Thread(r);
            th.add(t);
            t.start();
        }

        // Wait for the beans to reach the finish line
        startingLine.await(1000, TimeUnit.MILLISECONDS);

        //  -- SET --

        assertEquals(10, instances.get());

        //  -- GO --

        startPistol.countDown(); // go

        for (final Thread t : th) {
            t.join();
        }

        //  -- DONE --

        assertEquals(10, instances.get());

    }

    public void testStatelessBeanRelease() throws Exception {

        final int count = 30;
        final CountDownLatch invocations = new CountDownLatch(count);
        final InitialContext ctx = new InitialContext();
        final Runnable counterBeanLocal = new Runnable() {
            public void run() {

                Object object = null;
                try {
                    object = ctx.lookup("CounterBeanLocal");
                } catch (final NamingException e) {
                    assertTrue(false);
                }
                final Counter counter = (Counter) object;
                assertNotNull(counter);
                try {
                    counter.explode(invocations);
                } catch (final Exception e) {
                    //Ignore
                }
            }
        };

        // 'count' instances should be created and discarded.
        final Collection<Thread> th = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Thread thread = new Thread(counterBeanLocal);
            th.add(thread);
            thread.start();
        }

        for (final Thread t : th) {
            t.join();
        }
        assertEquals(count, discardedInstances.get());

    }


    public void testStatelessBeanTimeout() throws Exception {

        final InitialContext ctx = new InitialContext();
        final Object object = ctx.lookup("CounterBeanLocal");
        assertTrue("instanceof counter", object instanceof Counter);

        final CountDownLatch timeouts = new CountDownLatch(10);
        final CountDownLatch startPistol = new CountDownLatch(1);
        final CountDownLatch startingLine = new CountDownLatch(10);

        final Counter counter = (Counter) object;

        // Do a business method...
        final Runnable r = new Runnable() {
            public void run() {
                try {
                    counter.race(startingLine, startPistol);
                } catch (final ConcurrentAccessTimeoutException ex) {
                    comment("Leap Start");
                    timeouts.countDown();
                }
            }
        };


        comment("On your mark!");

        for (int i = 0; i < 20; i++) {
            final Thread t = new Thread(r);
            t.start();
        }

        // Wait for the beans to reach the start line
        assertTrue("expected 10 invocations", startingLine.await(3000, TimeUnit.MILLISECONDS));

        comment("Get Set!");

        // Wait for the other beans timeout
        assertTrue("expected 10 timeouts", timeouts.await(3000, TimeUnit.MILLISECONDS));

        assertEquals(10, instances.get(), 1.1);

        comment("Go!");

        startPistol.countDown(); // go
    }

    public static Object lock = new Object[]{};

    private static void comment(final String x) {
//        synchronized(lock){
//            System.out.println(x);
//            System.out.flush();
//        }
    }

    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY, InitContextFactory.class.getName());

        final ConfigurationFactory config = new ConfigurationFactory();
        final Assembler assembler = new Assembler();

        assembler.createProxyFactory(config.configureService(ProxyFactoryInfo.class));
        assembler.createTransactionManager(config.configureService(TransactionServiceInfo.class));
        assembler.createSecurityService(config.configureService(SecurityServiceInfo.class));

        // containers
        final StatelessSessionContainerInfo statelessContainerInfo = config.configureService(StatelessSessionContainerInfo.class);
        statelessContainerInfo.properties.setProperty("TimeOut", "100");
        statelessContainerInfo.properties.setProperty("MaxSize", "10");
        statelessContainerInfo.properties.setProperty("MinSize", "2");
        statelessContainerInfo.properties.setProperty("StrictPooling", "true");
        assembler.createContainer(statelessContainerInfo);

        // Setup the descriptor information

        final StatelessBean bean = new StatelessBean(CounterBean.class);
        bean.addBusinessLocal(Counter.class.getName());
        bean.addBusinessRemote(RemoteCounter.class.getName());
        bean.addPostConstruct("init");
        bean.addPreDestroy("destroy");

        final EjbJar ejbJar = new EjbJar();
        ejbJar.addEnterpriseBean(bean);

        instances.set(0);
        assembler.createApplication(config.configureApplication(ejbJar));
    }

    @Override
    protected void tearDown() throws Exception {
        OpenEJB.destroy();
    }

    public static interface Counter {
        int count();

        void race(CountDownLatch ready, CountDownLatch go);

        void explode(CountDownLatch latch);
    }

    @Remote
    public static interface RemoteCounter extends Counter {

    }

    public static enum Lifecycle {
        CONSTRUCTOR, INJECTION, POST_CONSTRUCT, BUSINESS_METHOD, PRE_DESTROY
    }

    @Stateless
    public static class CounterBean implements Counter, RemoteCounter {


        private final int count;

        public CounterBean() {
            count = instances.incrementAndGet();
        }

        public int count() {
            return instances.get();
        }

        public int discardCount() {
            return discardedInstances.get();
        }

        public void explode(final CountDownLatch latch) {
            discardedInstances.incrementAndGet();
            try {
                throw new NullPointerException("Test expected this null pointer: " + latch.getCount());
            } finally {
                latch.countDown();
            }
        }

        public void race(final CountDownLatch ready, final CountDownLatch go) {
            comment("ready = " + count);
            ready.countDown();
            try {
                go.await();
                comment("running = " + count);
            } catch (final InterruptedException e) {
                Thread.interrupted();
            }
        }

        public void init() {

        }

        public void destroy() {

        }
    }
}
