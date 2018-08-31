/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import org.springframework.util.AntPathMatcher;

/**
 * Matcher of group to ant-style pattern. We support * as arbitrary characters within group name, 
 * ** as arbitrary subgroups. E.g.:
 * <pre>
 * /tenants/** /users matches: /tenant/foo/bar/users and /tenant/foo/users
 * /tenants/grp* /users matches: /tenant/grpOther/users but not /tenant/grpOther/foo/users
 * </pre>
 * 
 * @author K. Benedyczak
 */
public class GroupPatternMatcher
{
	public static boolean matches(String group, String pattern)
	{
		AntPathMatcher matcher = new AntPathMatcher();
		return matcher.match(pattern, group);
	}
}
