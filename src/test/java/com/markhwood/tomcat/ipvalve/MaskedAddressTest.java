package com.markhwood.tomcat.ipvalve;

/*
 * #%L
 * Tomcat-IP-Valve
 * %%
 * Copyright (C) 2013 - 2015 Mark H. Wood
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mhwood
 */
public class MaskedAddressTest
{
    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of matches method, of class MaskedAddress.
     *
     * @throws java.net.UnknownHostException
     */
    @Test
    public void testMatches()
            throws UnknownHostException
    {
        System.out.println("matches");

        InetAddress comparand = InetAddress.getByName("12.34.99.123");

        MaskedAddress instance = new MaskedAddress("12.34.56.78", 16);
        boolean result = instance.matches(comparand);
        assertTrue("12.34.99.123 should match 12.34.56.78/16", result);

        comparand = InetAddress.getByName("10.0.0.1");
        result = instance.matches(comparand);
        assertFalse("10.0.0.1 should NOT match 12.34.56.78/16", result);

        // TODO lots more trials
    }
}
