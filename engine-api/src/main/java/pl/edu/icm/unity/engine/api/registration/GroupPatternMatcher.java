/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.AntPathMatcher;

import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam.IncludeGroupsMode;

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

	/**
	 * @return list of those Group objects from allGroups which are matching the given pattern
	 */
	public static List<Group> filterMatching(List<Group> allGroups, String pattern)
	{
		return allGroups.stream()
			.filter(grp -> matches(grp.toString(), pattern))
			.collect(Collectors.toList());
	}
	
	/**
	 * @return list of those Group objects from allGroups which are matching to the given group access mode
	 */
	public static List<Group> filterByIncludeGroupsMode(List<Group> allGroups, IncludeGroupsMode mode)
	{
		if (mode.equals(IncludeGroupsMode.all))
			return allGroups;
		return allGroups.stream().filter(g -> mode.equals(IncludeGroupsMode.publicOnly) ? g.isPublic() : !g.isPublic()).collect(Collectors.toList());
	}
	
	/**
	 * @return list of those Group objects from allGroups which are in filter list
	 */
	public static List<Group> filterMatching(List<Group> allGroups, Collection<String> filter)
	{
		Map<String, Group> groups = allGroups.stream().collect(Collectors.toMap(g -> g.toString(), g -> g));
		return filter.stream()
			.filter(grp -> groups.containsKey(grp))
			.map(grp -> groups.get(grp))
			.collect(Collectors.toList());
	}

	public static boolean isValidPattern(String groupPath)
	{
		return groupPath.startsWith("/");
	}
}
