/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.EntityParam;

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

	@Autowired
	public GroupMembersIE(ObjectMapper jsonMapper, GroupResolver groupResolver, DBGroups dbGroups)
	{
		super(jsonMapper);
		this.groupResolver = groupResolver;
		this.dbGroups = dbGroups;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
			IOException, IllegalTypeException
	{
		Map<String, GroupBean> sortedGroups = GroupsIE.getSortedGroups(sql, groupResolver); 
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);

		jg.writeStartArray();
		for (Map.Entry<String, GroupBean> entry: sortedGroups.entrySet())
		{
			List<BaseBean> members = mapper.getMembers(entry.getValue().getId());
			
			jg.writeStartObject();
			jg.writeStringField("groupPath", entry.getKey());
			jg.writeFieldName("members");
			jg.writeStartArray();
			for (BaseBean member: members)
				jg.writeNumber(member.getId());
			jg.writeEndArray();
			jg.writeEndObject();
		}
		jg.writeEndArray();
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
				if (!element.isNumeric())
					break;
				long memberId = input.getLongValue();
				dbGroups.addMemberFromParent(path, new EntityParam(memberId), sql);
			}
			JsonUtils.expect(input, JsonToken.END_ARRAY);
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
}
