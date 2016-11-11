/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.name.Ava;


/**
 * Static utility methods helping to extract data from {@link ExprNode} 
 */
public class LdapNodeUtils
{

	/**
	 * @return Whether the LDAP query a user search?
	 */
	public static boolean isUserSearch(LdapServerProperties configuration, ExprNode node)
	{
		if (node instanceof BranchNode)
		{
			BranchNode n = (BranchNode) node;
			for (ExprNode en : n.getChildren())
			{
				if (isUserSearch(configuration, en))
				{
					return true;
				}
			}
		} else if (node instanceof SimpleNode)
		{
			SimpleNode<?> sns = (SimpleNode<?>) node;
			// be more strict - we have to know either one of the aliases
//			if (sns.getAttribute().equals("objectClass"))
//			{
//				return sns.getValue().toString().equals("inetorgperson");
//			}

			String[] aliases = configuration.getValue(
				LdapServerProperties.USER_NAME_ALIASES
			).split(",");
			if (ArrayUtils.contains(aliases, sns.getAttribute()))
			{
				return true;
			}
		}
		return false;
	}


	public static String getUserName(LdapServerProperties configuration, Dn dn)
	{
		String[] aliases = configuration.getValue(
			LdapServerProperties.USER_NAME_ALIASES
		).split(",");
		for (String alias : aliases) {
			String part = getPart(dn, alias);
			if (null != part) {
				return part;
			}
		}
		return null;
	}

	public static String getUserName(LdapServerProperties configuration, ExprNode node)
	{
		String[] aliases = configuration.getValue(
			LdapServerProperties.USER_NAME_ALIASES
		).split(",");
		return getUserName(node, aliases);

	}

	//
	// private
	//

	private static String getUserName(ExprNode node, String attribute)
	{

		if (node instanceof BranchNode)
		{
			BranchNode n = (BranchNode) node;
			for (ExprNode en : n.getChildren())
			{
				String username = getUserName(en, attribute);
				if (null != username)
				{
					return username;
				}
			}
		} else if (node instanceof SimpleNode)
		{
			try
			{
				SimpleNode<?> sns = (SimpleNode<?>) node;
				if (sns.getAttribute().equals(attribute))
				{
					return sns.getValue().toString();
				}
			} catch (Exception e)
			{
				return null;
			}
		}
		return null;
	}

	// User name extracted from LDAP query based on attributes
	private static String getUserName(ExprNode node, String[] attributes)
	{
		for (int i = 0; i < attributes.length; ++i)
		{
			String uname = getUserName(node, attributes[i]);
			if (null != uname)
			{
				return uname;
			}
		}
		return null;
	}

	// Get query value from LDAP DN.
	public static String getPart(Dn dn, String query)
	{
		for (Rdn rdn : dn.getRdns())
		{
			Ava ava = rdn.getAva();
			if (null != ava && ava.getAttributeType().getName().equals(query))
			{
				return ava.getValue().getString();
			}
		}
		return null;
	}


}
