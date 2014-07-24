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
package org.apache.tomee.catalina.naming.resources;

import org.apache.naming.resources.FileDirContext;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.util.Date;

// a normal FileDirContext just unwrapping tomcat prefix
// to simulate a normal webapp dir and not a jar one
//
// this is close to VirtualDirContext excepted all are bound to the same endpoint (awesome in dev)
public class AdditionalDocBase extends FileDirContext {
    private static final String PREFIX = "/META-INF/resources";
    private static final int PREFIX_LENGTH = PREFIX.length();
    private static final String WEB_INF_CLASSES = "/WEB-INF/classes";
    private static final boolean RESPECT_HEADERS = Boolean.getBoolean("tomee.AdditionalDocBase.respect-headers");

    @Override
    protected File file(final String name) {
        if (name.startsWith(PREFIX)) {
            return super.file(name.substring(PREFIX_LENGTH));
        }
        if (WEB_INF_CLASSES.equals(name)) {
            return super.file("/");
        }
        return super.file(name);
    }

    @Override
    protected Attributes doGetAttributes(final String name, final String[] attrIds) throws NamingException {
        if (RESPECT_HEADERS) {
            return super.doGetAttributes(name, attrIds);
        }
        final File file = file(name);
        if (file == null) {
            return null;
        }
        return new ForceRefeshAttributes(file);
    }

    private class ForceRefeshAttributes extends FileResourceAttributes {
        public ForceRefeshAttributes(final File file) {
            super(file);
        }

        @Override
        public String getETag() {
            return null;
        }

        @Override
        public Date getLastModifiedDate() {
            return new Date();
        }

        @Override
        public long getLastModified() {
            return System.currentTimeMillis();
        }
    }
}
