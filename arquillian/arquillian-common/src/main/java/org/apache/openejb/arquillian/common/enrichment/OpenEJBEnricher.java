/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.arquillian.common.enrichment;

import org.apache.openejb.AppContext;
import org.apache.openejb.BeanContext;
import org.apache.openejb.InjectionProcessor;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.OpenEJBRuntimeException;
import org.apache.openejb.arquillian.common.mockito.MockitoEnricher;
import org.apache.openejb.core.Operation;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.OWBInjector;
import org.jboss.arquillian.test.spi.TestClass;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

public final class OpenEJBEnricher {
    private static final Logger LOGGER = Logger.getLogger(OpenEJBEnricher.class.getName());

    private OpenEJBEnricher() {
        // no-op
    }

    public static void enrich(final Object testInstance, final AppContext ctx) {
        // don't rely on arquillian since this enrichment should absolutely be done before the following ones
        new MockitoEnricher().enrich(testInstance);
        if (ctx == null) {
            return;
        }

        final BeanContext context = SystemInstance.get().getComponent(ContainerSystem.class).getBeanContext(ctx.getId() + "_" + testInstance.getClass().getName());

        final BeanManagerImpl bm = findBeanManager(ctx);
        if (bm != null && bm.isInUse()) {
            try {
                final Set<Bean<?>> beans = bm.getBeans(testInstance.getClass());
                final Bean<?> bean = bm.resolve(beans);
                final CreationalContext<?> cc = bm.createCreationalContext(bean);
                if (context != null) {
                    context.set(CreationalContext.class, cc);
                }
                OWBInjector.inject(bm, testInstance, cc);
            } catch (final Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed injection on: " + testInstance.getClass(), t);
                if (RuntimeException.class.isInstance(t)) {
                    throw RuntimeException.class.cast(t);
                }
                if (Exception.class.isInstance(t)) {
                    throw new OpenEJBRuntimeException(Exception.class.cast(t));
                }
                // ignoring other cases for the moment, let manage some OWB API change without making all tests failing
            }
        }

        if (context != null) {
            final ThreadContext callContext = new ThreadContext(context, null, Operation.INJECTION);
            final ThreadContext oldContext = ThreadContext.enter(callContext);
            try {
                final InjectionProcessor processor = new InjectionProcessor<>(testInstance, context.getInjections(), context.getJndiContext());
                processor.createInstance();
            } catch (final OpenEJBException e) {
                // ignored
            } finally {
                ThreadContext.exit(oldContext);
            }
        }
    }

    private static BeanManagerImpl findBeanManager(final AppContext ctx) {
        if (ctx != null) {
            return ctx.getWebBeansContext().getBeanManagerImpl();
        }

        try { // else try to find it from tccl through our SingletonService
            return WebBeansContext.currentInstance().getBeanManagerImpl();
        } catch (final Exception e) { // if not found IllegalStateException or a NPE can be thrown
            // no-op
        }

        return null;
    }

    public static Object[] resolve(final AppContext appContext, final TestClass ignored, final Method method) { // suppose all is a CDI bean...
        final Object[] values = new Object[method.getParameterTypes().length];

        if (appContext == null) {
            return values;
        }

        final BeanManagerImpl beanManager = findBeanManager(appContext);
        if (beanManager == null) {
            return values;
        }

        final Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            try {
                values[i] = getParamInstance(beanManager, i, method);
            } catch (final Exception e) {
                LOGGER.info(e.getMessage());
            }
        }
        return values;
    }

    private static <T> T getParamInstance(final BeanManagerImpl manager, final int position, final Method method) {
        final CreationalContextImpl<?> creational = manager.createCreationalContext(null);
        return (T) manager.getInjectableReference(new MethodParamInjectionPoint(method, position, manager), creational);
    }

    private static class MethodParamInjectionPoint implements InjectionPoint {
        private final Method method;
        private final int position;
        private final Set<Annotation> qualifiers = new HashSet<>();

        private MethodParamInjectionPoint(final Method method, final int position, final BeanManager beanManager) {
            this.method = method;
            this.position = position;

            for (final Annotation annotation : method.getParameterAnnotations()[position]) {
                if (beanManager.isQualifier(annotation.annotationType())) {
                    qualifiers.add(annotation);
                }
            }
            qualifiers.add(new DefaultLiteral());
            qualifiers.add(new AnyLiteral());
        }

        @Override
        public Type getType() {
            if (method.getGenericParameterTypes().length > 0) {
                return method.getGenericParameterTypes()[position];
            }
            return method.getParameterTypes()[position];
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return method;
        }

        @Override
        public Annotated getAnnotated() {
            return new ParamAnnotated(method, position);
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }
    }

    private static class ParamAnnotated implements AnnotatedParameter<Object> {
        private final Method method;
        private final int position;
        private final Set<Type> types = new HashSet<>();
        private final Set<Annotation> annotations;

        private ParamAnnotated(final Method method, final int position) {
            this.method = method;
            this.position = position;

            types.add(getBaseType());
            types.add(Object.class);

            annotations = new HashSet<Annotation>(asList(method.getParameterAnnotations()[position]));
        }

        @Override
        public int getPosition() {
            return position;
        }

        @Override
        public AnnotatedCallable<Object> getDeclaringCallable() {
            return null;
        }

        @Override
        public Type getBaseType() {
            if (method.getGenericParameterTypes().length > 0) {
                return method.getGenericParameterTypes()[position];
            }
            return method.getParameterTypes()[position];
        }

        @Override
        public Set<Type> getTypeClosure() {
            return types;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
            for (final Annotation a : annotations) {
                if (a.annotationType().getName().equals(annotationType.getName())) {
                    return (T) a;
                }
            }
            return null;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return annotations;
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }
    }
}
