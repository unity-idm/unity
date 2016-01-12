/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.json.GroupMembershipSerializer;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.model.GroupElementBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles import/export of group members table.
 * @author K. Benedyczak
 */
@Component
public class GroupMembersIE extends AbstractIE
{
	private final GroupResolver groupResolver;
	private final DBGroups dbGroups;
	private final GroupMembershipSerializer groupMembershipSerializer;

	@Autowired
	public GroupMembersIE(ObjectMapper jsonMapper, GroupResolver groupResolver, DBGroups dbGroups,
			GroupMembershipSerializer groupMembershipSerializer)
	{
		super(jsonMapper);
		this.groupResolver = groupResolver;
		this.dbGroups = dbGroups;
		this.groupMembershipSerializer = groupMembershipSerializer;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
			IOException, IllegalTypeException
	{
		Map<String, GroupBean> sortedGroups = GroupsIE.getSortedGroups(sql, groupResolver); 
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);

		jg.writeStartArray();
		for (Map.Entry<String, GroupBean> entry: sortedGroups.entrySet())
		{
			List<GroupElementBean> members = mapper.getMembers(entry.getValue().getId());
			serializeGroupElementBeans(jg, entry.getKey(), members);
		}
		jg.writeEndArray();
	}
	
	private void serializeGroupElementBeans(JsonGenerator jg, String groupPath, List<GroupElementBean> members)
			throws IOException
	{
		jg.writeStartObject();
		jg.writeStringField("groupPath", groupPath);
		jg.writeFieldName("members");
		jg.writeStartArray();
		for (GroupElementBean member: members)
			serializeGroupElementBean(jg, groupPath, member);
		jg.writeEndArray();
		jg.writeEndObject();
	}

	void serializeGroupElementBean(JsonGenerator jg, String groupPath, GroupElementBean member)
			throws IOException
	{
		jg.writeStartObject();				
		jg.writeNumberField("entity", member.getElementId());
		jg.writeNumberField("groupId", member.getGroupId());
		if (member.getContents() == null)
			jg.writeNullField("contents");
		else
			jg.writeBinaryField("contents", member.getContents());
		jg.writeEndObject();
	}
	
	public void deserialize(SqlSession sql, JsonParser input) throws IOException, EngineException
	{
		JsonUtils.expect(input, JsonToken.START_ARRAY);

		while (input.nextToken() == JsonToken.START_OBJECT)
		{
			JsonUtils.nextExpect(input, "groupPath");
			String path = input.getText();
			
			JsonUtils.nextExpect(input, "members");
			JsonUtils.expect(input, JsonToken.START_ARRAY);
			
			JsonToken element;
			while ((element = input.nextToken()) != null)
			{
				if (element.isNumeric())
					handleLegacyMembership(path, sql, input);
				else if (element.isStructStart())
					handleNewMembership(path, sql, input);
				else
					break;
			}
			JsonUtils.expect(input, JsonToken.END_ARRAY);
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
	
	GroupMembership deserializeMembershipInformation(String path, JsonParser input) throws IOException
	{
		JsonUtils.nextExpect(input, "entity");
		long entityId = input.getLongValue();
		JsonUtils.nextExpect(input, "groupId");
		input.getLongValue(); //we ignore this
		JsonUtils.nextExpect(input, "contents");
		
		byte[] contents = (input.getCurrentToken() == JsonToken.VALUE_NULL) ? null : input.getBinaryValue();
		return groupMembershipSerializer.fromJson(contents, entityId, path);
	}
	
	private void handleNewMembership(String path, SqlSession sql, JsonParser input)
			throws IOException, EngineException
	{
		GroupMembership parsed = deserializeMembershipInformation(path, input);
		dbGroups.addMemberFromParent(path, new EntityParam(parsed.getEntityId()), parsed.getRemoteIdp(), 
				parsed.getTranslationProfile(), parsed.getCreationTs(), sql);
		JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
	}

	private void handleLegacyMembership(String path, SqlSession sql, JsonParser input) 
			throws IOException, EngineException
	{
		long memberId = input.getLongValue();
		dbGroups.addMemberFromParent(path, new EntityParam(memberId), null, null, new Date(), sql);
	}
}
