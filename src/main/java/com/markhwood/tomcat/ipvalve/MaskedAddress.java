/*
 * Copyright 2013 Mark H. Wood.
 * MHW, 15-Mar-2013
 */

package com.markhwood.tomcat.ipvalve;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Holds an IPv4 or IPv6 address and a mask exposing leading significant bits,
 * and performs masked comparisons with other addresses.
 *
 * @author mhwood
 */
public class MaskedAddress
{
    private int maskBits;

    private BigInteger address;

    private BigInteger mask;
    private int addressSize;

    /**
     * Construct the IPv6 loopback address.
     *
     * @throws UnknownHostException when donkeys fly.
     */
    public MaskedAddress()
            throws UnknownHostException
    {
        setAddress("::1");
        setMask(128);
    }

    /**
     * Construct a matcher for an address, masked by some number of trailing bits.
     *
     * @param address IPv4 or IPv6 address acceptable to InetAddress.
     * @param maskBits CIDR mask width.
     * @throws UnknownHostException if {@link address} couldn't be parsed.
     */
    public MaskedAddress(String address, int maskBits)
            throws UnknownHostException
    {
        setAddress(address);
        setMask(maskBits);
    }

    /**
     * Compare a given address to this one, considering only unmasked bits to be
     * significant.
     *
     * @return
     */
    boolean matches(InetAddress comparand)
    {
        BigInteger candidate = new BigInteger(comparand.getAddress());
        System.out.printf("candidate:  %s\n", hexdump(candidate.toByteArray()));
        BigInteger difference = candidate.xor(address);
        System.out.printf("difference:  %s\n", hexdump(difference.toByteArray()));
        BigInteger significance = difference.and(mask);
        System.out.printf("significance:  %s\n", hexdump(significance.toByteArray()));
        return significance.equals(BigInteger.ZERO);
    }

    private String hexdump(byte[] in)
    {
        StringBuilder bupher = new StringBuilder();
        for (byte b : in)
        {
            bupher.append(String.format("%02x", b));
        }
        return bupher.toString();
    }

    public final void setAddress(String address)
            throws UnknownHostException
    {
        byte[] addressBits = InetAddress.getByName(address).getAddress();
        this.address = new BigInteger(addressBits);
        addressSize = addressBits.length;
    }

    byte[] getAddress() { return address.toByteArray(); }

    /**
     * Create a mask excluding trailing insignificant bits.
     *
     * @param maskBits Number of leading significant bits.  If negative, calculate
     *                  a mask with all one bits -- this matches only one address.
     */
    public final void setMask(int maskBits)
    {
        this.maskBits = maskBits;
        byte[] masq = new byte[addressSize]; // FIXME may not be set yet!
        if (maskBits < 0) maskBits = addressSize * 8;
        for (int i = 0; i < maskBits; i++)
        {
            int bit = 7-(i%8);
            byte bytemask = (byte) (1<<bit);
            masq[i/8] |= bytemask;
        }
        mask = new BigInteger(masq);
    }

    int getMask() { return maskBits; }
}
