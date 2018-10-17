/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.authn.CredentialInfo;

public class TestGroupMember
{
	@Test
	public void jsonSerializationIsIdempotent()
	{
		GroupMember gm = new GroupMember("/group", 
				new Entity(Collections.emptyList(), new EntityInformation(), 
						new CredentialInfo("credreq", new HashMap<>())), 
				Collections.emptyList());
		
		String string = JsonUtil.toJsonString(gm);
		
		GroupMember parsed = JsonUtil.parse(string, GroupMember.class);
		
		assertThat(parsed, is(gm));
	}
}
