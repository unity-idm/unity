/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;

import pl.edu.icm.unity.server.utils.Log;

/**
 * Simple helpers
 * @author K. Benedyczak
 */
public class LdapUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapUtils.class);
	
	/**
	 * Returns a value of the nameAttribute in dn. If not found then null is returned. This is intended 
	 * as in the code using this method this is a normal, not exceptional condition.
	 * @param nameAttribute
	 * @param dn
	 * @return
	 * @throws LDAPException if the dn is invalid
	 */
	public static String extractNameFromDn(String nameAttribute, String dn)
	{
		RDN[] rdns;
		try
		{
			rdns = DN.getRDNs(dn);
		} catch (LDAPException e)
		{
			log.warn("Found a string which is not a DN, what was expected. Most probably the LDAP " +
					"configuration is invalid wrt the schema used by the LDAP server. " +
					"Expected as DN: " + dn, e);
			return null;
		}
		for (RDN rdn: rdns)
		{
			String[] attrNames = rdn.getAttributeNames();
			String[] attrValues = rdn.getAttributeValues();
			if (attrNames.length == 1 && attrValues.length == 1 && attrNames[0].equals(nameAttribute))
			{
				return attrValues[0];
			}
		}
		return null;
	}
	
	public static String extractUsername(String username, Pattern extractorRegexp)
	{
		if (extractorRegexp == null)
			return username;
		Matcher matcher = extractorRegexp.matcher(username);
		
		if (!matcher.matches() || matcher.groupCount() < 1)
		{
			log.warn("Id extractor regexp doesn't returned any capturing group for " + username + 
					". Returning original value as username, but this can likely be wrong.");
			return username;
		}
		
		return matcher.group(1);
	}
	
	public static boolean nonEmpty(String a)
	{
		return a != null && !a.isEmpty();
	}
}
