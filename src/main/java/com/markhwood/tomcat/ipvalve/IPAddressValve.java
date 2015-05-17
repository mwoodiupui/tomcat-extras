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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RequestFilterValve;

/**
 * Permit or deny requests according to the client address, from a list of
 * address/mask specifications.  This is arguably a better fit to the way people
 * think about IP addresses than the regular-expression matching offered by
 * {@link org.apache.catalina.valves.RemoteAddrValve}.
 *
 * <p>
 * Configuration is as with other {@link RequestFilterValve} subclasses such as
 * RemoteAddrValve:  the Valve element's 'allow' and
 * 'deny' attributes supply patterns of addresses allowed and denied access.
 * Unlike that class, this one takes for each a whitespace-separated list of
 * address/mask rules.
 * </p>
 *
 * <p>
 * Configured rules are tested against the current source address, in order of
 * appearance.  First the deny rules are tested.  If a deny rule matches, the
 * request is denied.  If no deny rule matches, and there are no accept rules,
 * the request is accepted.  If an accept rule matches, the request is accepted.
 * Otherwise the request is denied.
 * </p>
 *
 * @author mhwood
 */
public class IPAddressValve
        extends RequestFilterValve
{
    private static final Logger log = Logger.getLogger(IPAddressValve.class.getName());

    /** Matchers for allowed address blocks. */
    List<MaskedAddress> allowPatterns = new ArrayList<>();

    /** Matchers for denied address blocks. */
    List<MaskedAddress> denyPatterns = new ArrayList<>();

    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    private static final String info
            = "Allow/deny requests by CIDR-masked IPv4 or IPv6 address.";

    @Override
    public String getInfo()
    {
        return info;
    }

    @Override
    public void setAllow(String allows)
    {
        log.log(Level.CONFIG, "setAllow {0}", allows);
        allowValid = false;
        allowValue = allows;
        try {
            allowPatterns = makePatterns(allows);
        } catch (Exception e) {
            log.log(Level.WARNING, "Invalid 'allow' rule in {0} : {1}",
                    new Object[]{allows, e.getMessage()});
            return;
        }
        allowValid = true;
    }

    @Override
    public void setDeny(String denies)
    {
        log.log(Level.CONFIG, "setDeny {0}", denies);
        denyValid = false;
        denyValue = denies;
        try {
            denyPatterns = makePatterns(denies);
        } catch (Exception e) {
            log.log(Level.WARNING, "Invalid 'deny' rule in {0} : {1}",
                    new Object[]{denies, e.getMessage()});
            return;
        }
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
                log.finest("Denied by rule");
                denyRequest(rqst, rspns);
                return;
            }
        }

        // If allowPatterns.empty() then allow the request.
        if (allowPatterns.isEmpty())
        {
            log.finest("Accepted by default");
            if (null != next)
                next.invoke(rqst, rspns);
            return;
        }

        // Not denied, so check allowed.
        for (MaskedAddress pattern : allowPatterns)
        {
            if (pattern.matches(remoteAddress))
            {
                log.finest("Accepted by rule");
                if (null != next)
                    next.invoke(rqst, rspns);
                return;
            }
        }

        // No match!
        log.finest("Denied by default");
        denyRequest(rqst, rspns);
    }

    /**
     * Parse a pattern list.
     *
     * @param ruleList whitespace-separated address-matching rules.
     * @return address patterns.
     * @throws UnknownHostException if address could not be parsed.
     * @throws IllegalArgumentException if more than one "/" occurs in a rule.
     */
    private static List<MaskedAddress> makePatterns(String ruleList)
            throws UnknownHostException
    {
        // Crack whitespace-separated pattern list.
        String[] rules = ruleList.split("\\s+");

        List<MaskedAddress> patterns = new ArrayList<>();
        for (String rule : rules)
        {
            log.log(Level.FINE, "pattern ''{0}''", rule);

            // Discard leading whitespace
            if (rule.isEmpty())
                continue;

            // Create a MaskedAddress from each pattern, if possible.
            String[] parts = rule.split("/", 2);
            log.log(Level.FINE, "parts ''{0}'' / ''{1}'' / ''{2}''", parts);
            MaskedAddress pattern;
            switch (parts.length)
            {
            //case 0 can't happen
            case 1:
                pattern = new MaskedAddress(parts[0], -1);
                patterns.add(pattern);
                break;
            case 2:
                pattern = new MaskedAddress(parts[0], Integer.valueOf(parts[1]));
                patterns.add(pattern);
                break;
            default:
                throw new IllegalArgumentException("Too many slashes in a rule:  " + rule);
            }
        }

        return patterns;
    }
}
