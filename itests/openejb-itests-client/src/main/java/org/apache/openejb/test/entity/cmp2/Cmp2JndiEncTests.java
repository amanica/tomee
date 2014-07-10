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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.test.entity.cmp2;

import org.apache.openejb.test.TestFailureException;
import org.apache.openejb.test.entity.cmp.EncCmpHome;
import org.apache.openejb.test.entity.cmp.EncCmpObject;

/**
 * [4] Should be run as the fourth test suite of the EncCmpTestClients
 */
public class Cmp2JndiEncTests extends Cmp2TestClient {

    protected EncCmpHome ejbHome;
    protected EncCmpObject ejbObject;

    public Cmp2JndiEncTests() {
        super("JNDI_ENC.");
    }

    protected void setUp() throws Exception {
        super.setUp();
        final Object obj = initialContext.lookup("client/tests/entity/cmp2/EncBean");
        ejbHome = (EncCmpHome) javax.rmi.PortableRemoteObject.narrow(obj, EncCmpHome.class);
        ejbObject = ejbHome.create("Enc Bean");
    }

    protected void tearDown() throws Exception {
        try {
            //ejbObject.remove();
        } catch (final Exception e) {
            throw e;
        } finally {
            super.tearDown();
        }
    }

    public void test01_lookupStringEntry() {
        try {
            ejbObject.lookupStringEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test02_lookupDoubleEntry() {
        try {
            ejbObject.lookupDoubleEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test03_lookupLongEntry() {
        try {
            ejbObject.lookupLongEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test04_lookupFloatEntry() {
        try {
            ejbObject.lookupFloatEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test05_lookupIntegerEntry() {
        try {
            ejbObject.lookupIntegerEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test06_lookupShortEntry() {
        try {
            ejbObject.lookupShortEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test07_lookupBooleanEntry() {
        try {
            ejbObject.lookupBooleanEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test08_lookupByteEntry() {
        try {
            ejbObject.lookupByteEntry();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test09_lookupEntityBean() {
        try {
            ejbObject.lookupEntityBean();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test10_lookupStatefulBean() {
        try {
            ejbObject.lookupStatefulBean();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test11_lookupStatelessBean() {
        try {
            ejbObject.lookupStatelessBean();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test12_lookupResource() {
        try {
            ejbObject.lookupResource();
        } catch (final TestFailureException e) {
            throw e.error;
        } catch (final Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    public void test13_lookupPersistenceUnit() {
        try{
            ejbObject.lookupPersistenceUnit();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }
    
    public void test14_lookupPersistenceContext() {
        try{
            ejbObject.lookupPersistenceContext();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }

    public void test19_lookupStatelessBusinessLocal() {
        try{
            ejbObject.lookupStatelessBusinessLocal();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }

    public void test20_lookupStatelessBusinessRemote() {
        try{
            ejbObject.lookupStatelessBusinessRemote();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }

    public void test21_lookupStatefulBusinessLocal() {
        try{
            ejbObject.lookupStatefulBusinessLocal();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }

    public void test22_lookupStatefulBusinessRemote() {
        try{
            ejbObject.lookupStatefulBusinessRemote();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }
    
    public void test23_lookupJMSConnectionFactory() {
        try{
            ejbObject.lookupJMSConnectionFactory();
        } catch (final TestFailureException e){
            throw e.error;
        } catch (final Exception e){
            fail("Received Exception "+e.getClass()+ " : "+e.getMessage());
        }
    }
}
