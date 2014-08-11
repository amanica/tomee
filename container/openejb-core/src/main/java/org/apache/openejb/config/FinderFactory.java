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

package org.apache.openejb.config;

import org.apache.openejb.OpenEJBRuntimeException;
import org.apache.openejb.jee.Beans;
import org.apache.openejb.jee.EnterpriseBean;
import org.apache.openejb.loader.SystemInstance;
import org.apache.xbean.finder.Annotated;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.IAnnotationFinder;
import org.apache.xbean.finder.UrlSet;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.apache.xbean.finder.archive.ClasspathArchive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.apache.openejb.util.Classes.ancestors;

public class FinderFactory {

    private static final FinderFactory factory = new FinderFactory();
    public static final String FORCE_LINK = "openejb.finder.force.link";
    private static volatile boolean MODULE_LIMITED = "true".equalsIgnoreCase(SystemInstance.get().getProperty("openejb.finder.module-scoped", "false"));

    private static FinderFactory get() {
        final FinderFactory factory = SystemInstance.get().getComponent(FinderFactory.class);
        return factory != null ? factory : FinderFactory.factory;
    }

    public static IAnnotationFinder createFinder(final DeploymentModule module) throws Exception {
        return get().create(module);
    }

    public static AnnotationFinder getFinder(final ClassLoader classLoader, final URL url) {
        return newFinder(ClasspathArchive.archive(classLoader, url));
    }

    public IAnnotationFinder create(final DeploymentModule module) throws Exception {
        final AnnotationFinder finder;
        if (module instanceof WebModule) {
            final WebModule webModule = (WebModule) module;
            finder = newFinder(new WebappAggregatedArchive(webModule, webModule.getScannableUrls())).link();
        } else if (module instanceof ConnectorModule) {
            final ConnectorModule connectorModule = (ConnectorModule) module;
            finder = newFinder(new ConfigurableClasspathArchive(connectorModule, connectorModule.getLibraries())).link();
        } else if (module instanceof AppModule) {
            final Collection<URL> urls = NewLoaderLogic.applyBuiltinExcludes(new UrlSet(AppModule.class.cast(module).getAdditionalLibraries())).getUrls();
            finder = newFinder(new WebappAggregatedArchive(module.getClassLoader(), module.getAltDDs(), urls));
        } else if (module.getJarLocation() != null) {
            final String location = module.getJarLocation();
            final File file = new File(location);

            URL url;
            if (file.exists()) {
                url = file.toURI().toURL();

                final File webInfClassesFolder = new File(file, "WEB-INF/classes"); // is it possible?? normally no
                if (webInfClassesFolder.exists() && webInfClassesFolder.isDirectory()) {
                    url = webInfClassesFolder.toURI().toURL();
                }
            } else {
                url = new URL(location);
            }

            if (module instanceof Module) {
                final DebugArchive archive = new DebugArchive(new ConfigurableClasspathArchive((Module) module, url));
                finder = newFinder(archive);
            } else {
                finder = newFinder(new DebugArchive(new ConfigurableClasspathArchive(module.getClassLoader(), url)));
            }
            finder.link();
        } else {
            // TODO: error. Here it means we'll not find anything so helping a bit (if you hit it outside a test fix it)
            finder = new AnnotationFinder(new ClassesArchive(ensureMinimalClasses(module))).enableMetaAnnotations();
        }

        return MODULE_LIMITED ? new ModuleLimitedFinder(finder) : finder;
    }

    private Class<?>[] ensureMinimalClasses(final DeploymentModule module) {
        if (EjbModule.class.isInstance(module)) {
            final Collection<Class<?>> finderClasses = new HashSet<>();

            final EjbModule ejb = EjbModule.class.cast(module);
            final EnterpriseBean[] enterpriseBeans = ejb.getEjbJar().getEnterpriseBeans();

            ClassLoader classLoader = ejb.getClassLoader();
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }

            for (final EnterpriseBean bean : enterpriseBeans) {
                try {
                    finderClasses.addAll(ancestors(classLoader.loadClass(bean.getEjbClass())));
                } catch (final ClassNotFoundException e) {
                    // no-op
                }
            }

            final Beans beans = ejb.getBeans();
            if (beans != null && ejb.getEjbJar() != null) {
                for (final List<String> managedClasses : beans.getManagedClasses().values()) {
                    for (final String name : managedClasses) {
                        try {
                            finderClasses.addAll(ancestors(classLoader.loadClass(name)));
                        } catch (final ClassNotFoundException e) {
                            // no-op
                        }
                    }
                }
                for (final String name : beans.getInterceptors()) {
                    try {
                        finderClasses.addAll(ancestors(classLoader.loadClass(name)));
                    } catch (final ClassNotFoundException e) {
                        // no-op
                    }
                }
                for (final String name : beans.getAlternativeClasses()) {
                    try {
                        finderClasses.addAll(ancestors(classLoader.loadClass(name)));
                    } catch (final ClassNotFoundException e) {
                        // no-op
                    }
                }
                for (final String name : beans.getDecorators()) {
                    try {
                        finderClasses.addAll(ancestors(classLoader.loadClass(name)));
                    } catch (final ClassNotFoundException e) {
                        // no-op
                    }
                }
            }

            return finderClasses.toArray(new Class<?>[finderClasses.size()]);
        }
        return new Class<?>[0];
    }

    private static AnnotationFinder newFinder(final Archive archive) {
        return new OpenEJBAnnotationFinder(archive);
    }

    public static final class DebugArchive implements Archive {
        private final Archive archive;

        private DebugArchive(final Archive archive) {
            this.archive = archive;
        }

        @Override
        public Iterator<Entry> iterator() {
            return archive.iterator();
        }

        @Override
        public InputStream getBytecode(final String s) throws IOException, ClassNotFoundException {
            return archive.getBytecode(s);
        }

        @Override
        public Class<?> loadClass(final String s) throws ClassNotFoundException {
            try {
                return archive.loadClass(s);
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public static class ModuleLimitedFinder implements IAnnotationFinder {
        private final IAnnotationFinder delegate;

        public ModuleLimitedFinder(final IAnnotationFinder delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotation) {
            return delegate.isAnnotationPresent(annotation);
        }

        @Override
        public List<String> getClassesNotLoaded() {
            return delegate.getClassesNotLoaded();
        }

        @Override
        public List<Package> findAnnotatedPackages(final Class<? extends Annotation> annotation) {
            return delegate.findAnnotatedPackages(annotation);
        }

        @Override
        public List<Class<?>> findAnnotatedClasses(final Class<? extends Annotation> annotation) {
            try {
                return filter(delegate.findAnnotatedClasses(annotation), new ClassPredicate<>(getAnnotatedClassNames()));
            } catch (final TypeNotPresentException tnpe) {
                throw handleException(tnpe, annotation);
            }
        }

        private RuntimeException handleException(final TypeNotPresentException tnpe, final Class<? extends Annotation> annotation) {
            try {
                final Method mtd = AnnotationFinder.class.getDeclaredMethod("getAnnotationInfos", String.class);
                mtd.setAccessible(true);
                final List<?> infos = (List<?>) mtd.invoke(delegate);
                for (final Object info : infos) {
                    if (info instanceof AnnotationFinder.ClassInfo) {
                        final AnnotationFinder.ClassInfo classInfo = (AnnotationFinder.ClassInfo) info;
                        try {
                            // can throw the exception
                            classInfo.get().isAnnotationPresent(annotation);
                        } catch (final TypeNotPresentException tnpe2) {
                            throw new OpenEJBRuntimeException("Missing type for annotation " + annotation.getName() + " on class " + classInfo.getName(), tnpe2);
                        } catch (final ThreadDeath ignored) {
                            // no-op
                        }
                    }
                }
            } catch (final Throwable th) {
                // no-op
            }
            return tnpe;
        }

        @Override
        public List<Class<?>> findInheritedAnnotatedClasses(final Class<? extends Annotation> annotation) {
            return filter(delegate.findInheritedAnnotatedClasses(annotation), new ClassPredicate<>(getAnnotatedClassNames()));
        }

        @Override
        public List<Method> findAnnotatedMethods(final Class<? extends Annotation> annotation) {
            return filter(delegate.findAnnotatedMethods(annotation), new MethodPredicate(getAnnotatedClassNames()));
        }

        @Override
        public List<Constructor> findAnnotatedConstructors(final Class<? extends Annotation> annotation) {
            return filter(delegate.findAnnotatedConstructors(annotation), new ConstructorPredicate(getAnnotatedClassNames()));
        }

        @Override
        public List<Field> findAnnotatedFields(final Class<? extends Annotation> annotation) {
            return filter(delegate.findAnnotatedFields(annotation), new FieldPredicate(getAnnotatedClassNames()));
        }

        @Override
        public List<Class<?>> findClassesInPackage(final String packageName, final boolean recursive) {
            return filter(delegate.findClassesInPackage(packageName, recursive), new ClassPredicate<>(getAnnotatedClassNames()));
        }

        @Override
        public <T> List<Class<? extends T>> findSubclasses(final Class<T> clazz) {
            return filter(delegate.findSubclasses(clazz), new ClassPredicate<T>(getAnnotatedClassNames()));
        }

        @Override
        public <T> List<Class<? extends T>> findImplementations(final Class<T> clazz) {
            return filter(delegate.findImplementations(clazz), new ClassPredicate<T>(getAnnotatedClassNames()));
        }

        @Override
        public List<Annotated<Method>> findMetaAnnotatedMethods(final Class<? extends Annotation> annotation) {
            return filter(delegate.findMetaAnnotatedMethods(annotation), new AnnotatedMethodPredicate(getAnnotatedClassNames()));
        }

        @Override
        public List<Annotated<Field>> findMetaAnnotatedFields(final Class<? extends Annotation> annotation) {
            return filter(delegate.findMetaAnnotatedFields(annotation), new AnnotatedFieldPredicate(getAnnotatedClassNames()));
        }

        @Override
        public List<Annotated<Class<?>>> findMetaAnnotatedClasses(final Class<? extends Annotation> annotation) {
            try {
                return filter(delegate.findMetaAnnotatedClasses(annotation), new AnnotatedClassPredicate(getAnnotatedClassNames()));
            } catch (final TypeNotPresentException tnpe) {
                throw handleException(tnpe, annotation);
            }
        }

        @Override
        public List<String> getAnnotatedClassNames() {
            return delegate.getAnnotatedClassNames();
        }

        private static <T> List<T> filter(final List<T> list, final Predicate<T> predicate) {
            final List<T> ts = new ArrayList<>();
            for (final T t : list) {
                if (predicate.accept(t)) {
                    ts.add(t);
                }
            }
            return ts;
        }

        public IAnnotationFinder getDelegate() {
            return delegate;
        }

        private abstract static class Predicate<T> {
            protected final List<String> accepted;

            public Predicate(final List<String> list) {
                accepted = list;
            }

            protected boolean accept(final T t) {
                return accepted.contains(name(t));
            }

            protected abstract String name(T t);
        }

        private static class ClassPredicate<T> extends Predicate<Class<? extends T>> {
            public ClassPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Class<? extends T> aClass) {
                return aClass.getName();
            }
        }

        private static class MethodPredicate extends Predicate<Method> {
            public MethodPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Method method) {
                return method.getDeclaringClass().getName();
            }
        }

        private static class FieldPredicate extends Predicate<Field> {
            public FieldPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Field field) {
                return field.getDeclaringClass().getName();
            }
        }

        private static class ConstructorPredicate extends Predicate<Constructor> {
            public ConstructorPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Constructor constructor) {
                return constructor.getDeclaringClass().getName();
            }
        }

        private static class AnnotatedClassPredicate extends Predicate<Annotated<Class<?>>> {
            public AnnotatedClassPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Annotated<Class<?>> aClass) {
                return aClass.get().getName();
            }
        }

        private static class AnnotatedMethodPredicate extends Predicate<Annotated<Method>> {
            public AnnotatedMethodPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Annotated<Method> method) {
                return method.get().getDeclaringClass().getName();
            }
        }

        private static class AnnotatedFieldPredicate extends Predicate<Annotated<Field>> {
            public AnnotatedFieldPredicate(final List<String> list) {
                super(list);
            }

            @Override
            protected String name(final Annotated<Field> field) {
                return field.get().getDeclaringClass().getName();
            }
        }
    }

    public static class OpenEJBAnnotationFinder extends AnnotationFinder {
        private static final String[] JVM_SCANNING_CONFIG = SystemInstance.get().getProperty("openejb.scanning.xbean.jvm", "java.").split(",");

        public OpenEJBAnnotationFinder(final Archive archive) {
            super(archive);
        }

        @Override
        protected boolean isJvm(final String name) {
            return sharedIsJvm(name);
        }

        // don't reuse URLClassLoaderFirst one since this one can kill scanning perf
        // using a raw but efficient impl
        public static boolean sharedIsJvm(final String name) {
            for (final String s : JVM_SCANNING_CONFIG) {
                if (name.startsWith(s)) {
                    return true;
                }
            }
            return false;
        }
    }
}
