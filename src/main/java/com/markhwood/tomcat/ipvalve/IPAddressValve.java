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
import javax.servlet.ServletException;
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
    List<MaskedAddress> allowPatterns = new ArrayList<>();
    List<MaskedAddress> denyPatterns = new ArrayList<>();

    @Override
    public void setAllow(String allows)
    {
        containerLog.debug("setAllow " + allows);
        allowValid = false;
        allowValue = allows;
        allowPatterns = makePatterns(allows);
        allowValid = true;
    }

    @Override
    public void setDeny(String denies)
    {
        containerLog.debug("setDeny " + denies);
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
                containerLog.debug("Denied");
                denyRequest(rqst, rspns);
                return;
            }
        }

        // If allowPatterns.empty() then allow the request.
        if (allowPatterns.isEmpty())
        {
            containerLog.debug("Accepted by default");
            if (null != next)
                next.invoke(rqst, rspns);
            return;
        }

        // Not denied, so check allowed.
        for (MaskedAddress pattern : allowPatterns)
        {
            if (pattern.matches(remoteAddress))
            {
            if (null != next)
                next.invoke(rqst, rspns);
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
