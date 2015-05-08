/*
 * Copyright 2013 Mark H. Wood.
 */

package com.markhwood.tomcat.ipvalve;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RequestFilterValve;

/**
 * Permit or deny requests according to the client address, from a list of
 * address/mask specifications.  This is arguably a better fit to the way people
 * think about IP addresses than the regular-expression matching offered by
 * {@link org.apache.catalina.valves.RemoteAddrValve}.
 * <p>
 * Configured rules are tested against the current source address, in order of
 * appearance.  The first rule which permits the address will cause it to be
 * accepted.  If no rule permits the address, the request is denied.  If no
 * rules have been configured, the address is accepted.
 * </p>
 *
 * @author mhwood
 */
public class IPAddressValve
        extends RequestFilterValve
{
    List<MaskedAddress> allowPatterns;
    List<MaskedAddress> denyPatterns;

    @Override
    public void setAllow(String allows)
    {
        allowValid = false;
        allowValue = allows;
        allowPatterns = makePatterns(allows);
        allowValid = true;
    }

    @Override
    public void setDeny(String denies)
    {
        denyValid = false;
        denyValue = denies;
        denyPatterns = makePatterns(denies);
        denyValid = true;
    }

    @Override
    public void invoke(Request rqst, Response rspns)
            throws IOException, ServletException
    {
        String remoteAddressString = rqst.getRemoteAddr();
        InetAddress remoteAddress = InetAddress.getByName(
                remoteAddressString);

        // Is this address denied access?
        for (MaskedAddress pattern : denyPatterns)
        {
            if (pattern.matches(remoteAddress))
            {
                denyRequest(rqst, rspns);
            }
        }

        // If allowPatterns.empty() then allow the request.
        if (allowPatterns.isEmpty())
            return;

        // Not denied, so check allowed.
        for (MaskedAddress pattern : allowPatterns)
        {
            if (pattern.matches(remoteAddress))
            {
                if (null != next)
                {
                    next.invoke(rqst, rspns);
                }
                return;
            }
        }

        // No match!
        denyRequest(rqst, rspns);
    }

    @Override
    public String getInfo()
    {
        return "Allow/deny requests by CIDR-masked IPv4 or IPv6 address.";
    }

    /**
     * Parse a pattern list.
     *
     * @param ruleList whitespace-separated address-matching rules.
     * @return address patterns.
     */
    private static List<MaskedAddress> makePatterns(String ruleList)
    {
        // Crack whitespace-separated pattern list.
        String[] rules = ruleList.split("\\s+");

        List<MaskedAddress> patterns = new ArrayList<>();
        for (String rule : rules)
        {
            try {
                // Create a MaskedAddress from each pattern, if possible.
                String[] parts = rule.split("/", 2);
                MaskedAddress pattern;
                switch (parts.length)
                {
                case 0:
                    pattern = new MaskedAddress(parts[0], -1);
                    patterns.add(pattern);
                    break;
                case 1:
                    pattern = new MaskedAddress(parts[0], Integer.valueOf(parts[1]));
                    patterns.add(pattern);
                    break;
                default:
                    // TODO complain:  too many slashes
                }
            } catch (UnknownHostException ex) {
                // TODO complain:  didn't parse
            }
        }

        return patterns;
    }
}
