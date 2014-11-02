package org.apache.openejb.tests.embedded;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;

import org.apache.openejb.tests.embedded.DummySingleton;

@Singleton
@LocalBean
public class DummySingletonExtension extends DummySingleton
{


    // @EJB
    // private DummySingleton dummySingleton;

}
