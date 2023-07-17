/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.SearchResultEntry;

import pl.edu.icm.unity.ldap.client.LdapGroupHelper;
import pl.edu.icm.unity.ldap.client.config.GroupSpecification;

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
		
		assertThat(filter).isEqualTo("(|"
				+ "(&(objectClass=gClass)(memberUid=Gloria))"
				+ "(objectClass=gClass2)"
				+ "(&(objectClass=gClass3)(memberDN=CN=Gloria Moria))"
				+ ")");
	}
}
