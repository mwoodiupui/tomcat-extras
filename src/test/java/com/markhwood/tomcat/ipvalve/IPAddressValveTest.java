/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.markhwood.tomcat.ipvalve;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 *
 * @author mhwood
 */
public class IPAddressValveTest
        extends TestCase
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Test of invoke method, of class IPAddressValve.
     * @throws java.lang.Exception
     */
    public void testInvoke()
            throws Exception
    {
        System.out.println("invoke");

        Request rqst = new Request();
        Response rspns = new MockResponse();
        rspns.setRequest(rqst);

        IPAddressValve instance = new IPAddressValve();

        List<MaskedAddress> rules = new ArrayList<>();
        rules.add(new MaskedAddress("10.0.0.1", 24));
        rules.add(new MaskedAddress("192.168.0.1", 16));
        instance.rules = rules;
        // TODO add a mock "next" Valve

        rqst.setRemoteAddr("134.68.171.23");
        rspns.recycle();
        instance.invoke(rqst, rspns);
        assertTrue("134.68.171.23 should be denied", rspns.isError());

        rqst.setRemoteAddr("10.0.0.19");
        rspns.recycle();
        instance.invoke(rqst, rspns);
        assertFalse("10.0.0.19 should be permitted", rspns.isError());

        // TODO lots more trials
    }

    private class MockResponse
            extends Response
    {
        private int status;

        @Override
        public void sendError(int status)
        {
            this.status = status;
        }

        @Override
        public int getStatus() { return status; }

        @Override
        public boolean isError() { return status >= 300; }

        @Override
        public void recycle() { status = 200; }
    }
}
