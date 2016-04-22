/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.SearchResultEntry;

import pl.edu.icm.unity.ldap.client.GroupSpecification;
import pl.edu.icm.unity.ldap.client.LdapGroupHelper;

public class GroupFilterTest
{
	@Test
	public void remoteSearchFilterIsValid()
	{
		LdapGroupHelper groupHelper = new LdapGroupHelper();
		
		List<GroupSpecification> gss = Lists.newArrayList(
				new GroupSpecification("gClass", "memberUid", 
						"groupNameA", "uid"),
				new GroupSpecification("gClass2", null, 
						"groupNameA", null),
				new GroupSpecification("gClass3", "memberDN", 
						"groupNameA", null));
		SearchResultEntry userEntry = new SearchResultEntry("CN=Gloria Moria", 
				new Attribute[] {new Attribute("uid", "Gloria")});

		String filter = groupHelper.buildGroupFilter(userEntry, gss, true);
		
		assertThat(filter, is("(|"
				+ "(&(objectClass=gClass)(memberUid=Gloria))"
				+ "(objectClass=gClass2)"
				+ "(&(objectClass=gClass3)(memberDN=CN=Gloria Moria))"
				+ ")"));
	}
}
