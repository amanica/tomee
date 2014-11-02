
package org.apache.openejb.tests.embedded;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
@LocalBean
public class DummySingleton {
    public void doNothing() {
    }
}
