/*
 * Copyright 2013 Mark H. Wood.
 * MHW, 17-Mar-2013
 */

package com.markhwood.tomcat.ipvalve;

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
        assertTrue("10.0.0.1 should NOT match 12.34.56.78/16", result);

        // TODO lots more trials
    }
}
