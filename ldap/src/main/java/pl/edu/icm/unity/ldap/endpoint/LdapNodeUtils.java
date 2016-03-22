/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;

/**
 * Static utility methods helping to extract data from {@link ExprNode} 
 */
public class LdapNodeUtils
{

	/**
	 * @return Whether the LDAP query a user search?
	 */
	public static boolean isUserSearch(ExprNode node)
	{
		if (node instanceof BranchNode)
		{
			BranchNode n = (BranchNode) node;
			for (ExprNode en : n.getChildren())
			{
				if (isUserSearch(en))
				{
					return true;
				}
			}
		} else if (node instanceof SimpleNode)
		{
			SimpleNode<?> sns = (SimpleNode<?>) node;
			if (sns.getAttribute().equals("objectClass"))
			{
				return sns.getValue().toString().equals("inetorgperson");
			}
			//FIXME this is clearly wrong
			if (sns.getAttribute().equals("mail"))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return User name extracted from LDAP query
	 */
	public static String getUserName(ExprNode node, String[] attributes)
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
}
