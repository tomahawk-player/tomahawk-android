package org.tomahawk.tomahawk_android.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import android.test.suitebuilder.TestSuiteBuilder;

public class TomahawkTests extends TestSuite {

    public static Test suite() {
        return new TestSuiteBuilder(TomahawkTests.class).includeAllPackagesUnderHere().build();
    }
}
