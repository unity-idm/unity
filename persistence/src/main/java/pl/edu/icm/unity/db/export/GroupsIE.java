/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.json.GroupsSerializer;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Group;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles import/export of groups table.
 * @author K. Benedyczak
 */
@Component
public class GroupsIE extends AbstractIE
{
	private final GroupResolver groupResolver;
	private final DBGroups dbGroups;
	private final GroupsSerializer groupsSerializer;

	@Autowired
	public GroupsIE(ObjectMapper jsonMapper, GroupResolver groupResolver, DBGroups dbGroups,
			GroupsSerializer groupsSerializer)
	{
		super(jsonMapper);
		this.groupResolver = groupResolver;
		this.dbGroups = dbGroups;
		this.groupsSerializer = groupsSerializer;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
			IOException, IllegalTypeException
	{
		Map<String, GroupBean> sortedGroups = getSortedGroups(sql, groupResolver); 
		
		jg.writeStartArray();
		for (Map.Entry<String, GroupBean> entry: sortedGroups.entrySet())
		{
			jg.writeStartObject();
			serializeBaseBeanToJson(jg, entry.getValue());
			jg.writeStringField("groupPath", entry.getKey());
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}
	
	public void deserialize(SqlSession sql, JsonParser input) throws IOException, EngineException
	{
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);
		AttributesMapper attributeMapper = sql.getMapper(AttributesMapper.class);
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			GroupBean bean = new GroupBean();
			deserializeBaseBeanFromJson(input, bean);
			JsonUtils.nextExpect(input, "groupPath");
			String path = input.getValueAsString();
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			
			if (path.equals("/"))
			{
				bean.setName(GroupResolver.ROOT_GROUP_NAME);
				mapper.insertGroup(bean);
			} else
			{
				Group toAdd =  new Group(path);
				groupsSerializer.fromJson(bean.getContents(), toAdd, mapper, attributeMapper);
				dbGroups.addGroup(toAdd, sql);
			}
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
	
	/**
	 * Overriden to fix empty description
	 */
	@Override
	protected void deserializeBaseBeanFromJson(JsonParser input, BaseBean at) throws IOException
	{
		JsonUtils.nextExpect(input, "id");
		at.setId(input.getLongValue());
		JsonUtils.nextExpect(input, "name");
		at.setName(input.getText());
		JsonUtils.nextExpect(input, "contents");
		if (input.getCurrentToken() == JsonToken.VALUE_NULL)
		{
			at.setContents(null);
		} else
		{
			ObjectNode parsed = jsonMapper.readTree(input);
			JsonNode desc = parsed.get("description");
			if (desc == null || desc.isNull())
				parsed.put("description", "");
			
			at.setContents(jsonMapper.writeValueAsBytes(parsed));
		}		
	}
	
	public static Map<String, GroupBean> getSortedGroups(SqlSession sql, GroupResolver groupResolver)
	{
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);
		List<GroupBean> beans = mapper.getAllGroups();
		Map<String, GroupBean> sortedGroups = new TreeMap<>(new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2)
			{
				if (o1.length() >= o2.length())
					return 1;
				else if (o1.length() < o2.length())
					return -1;
				else
					return o1.compareTo(o2);
			}

		});
		for (GroupBean bean: beans)
		{
			String path = groupResolver.resolveGroupPath(bean, mapper);
			sortedGroups.put(path, bean);
		}
		return sortedGroups;
	}
}
