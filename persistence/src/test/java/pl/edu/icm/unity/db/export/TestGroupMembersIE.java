/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.json.GroupMembershipSerializer;
import pl.edu.icm.unity.db.model.GroupElementBean;
import pl.edu.icm.unity.types.basic.GroupMembership;

public class TestGroupMembersIE
{
	@Test
	public void shouldDeserializeWithNullContents() throws IOException
	{
		GroupMembershipSerializer gmSerializer = new GroupMembershipSerializer();
		ObjectMapper mapper = new ObjectMapper();
		
		GroupMembersIE gmIE = new GroupMembersIE(mapper, null, null, gmSerializer);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JsonGenerator jg = mapper.getFactory().createGenerator(os);
		
		GroupElementBean data = new GroupElementBean(33, 123);
		
		gmIE.serializeGroupElementBean(jg, "/A", data);
		jg.close();

		String json = os.toString(StandardCharsets.UTF_8.name());
		
		JsonParser input = mapper.getFactory().createParser(json);
		
		input.nextToken();
		GroupMembership deserialized = gmIE.deserializeMembershipInformation("/A", input);
		
		assertThat(deserialized.getRemoteIdp(), is(nullValue()));
		assertThat(deserialized.getEntityId(), is(123L));
		assertThat(deserialized.getGroup(), is("/A"));
		assertThat(deserialized.getTranslationProfile(), is(nullValue()));
	}
}
