package org.apache.openejb.tests.embedded;

import java.util.Properties;

import javax.ejb.EJB;

import org.apache.openejb.api.LocalClient;
import org.junit.Test;

@LocalClient
public class FinderModuleScopedTest extends AOpenEjbTest {
    @EJB
    private DummySingletonExtension dummySingletonExtension;

    protected void setProperties(Properties properties) {
        super.setProperties(properties);
        properties.setProperty("openejb.validation.output.level", "VERBOSE");
    }

    @Test
    public void defaultsToTrue() throws Exception {
        dummySingletonExtension.doNothing();
    }

}
