/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.bulk;

import java.nio.charset.StandardCharsets;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@link ScheduledProcessingRule}s.
 *   
 * @author K. Benedyczak
 */
@Component
public class ProcessingRuleHandler extends DefaultEntityHandler<ScheduledProcessingRule> 
{
	public static final String PROCESSING_RULE_OBJECT_TYPE = "processingRule";
	private TranslationActionsRegistry actionsRegistry;
	
	@Autowired
	public ProcessingRuleHandler(ObjectMapper jsonMapper, TranslationActionsRegistry actionsRegistry)
	{
		super(jsonMapper, PROCESSING_RULE_OBJECT_TYPE, ScheduledProcessingRule.class);
		this.actionsRegistry = actionsRegistry;
	}

	@Override
	public GenericObjectBean toBlob(ScheduledProcessingRule value, SqlSession sql)
	{
		ObjectNode jsonNode = value.toJson(jsonMapper);
		String json;
		try
		{
			json = jsonMapper.writeValueAsString(jsonNode);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize scheduled processing rule to JSON", e);
		}
		return new GenericObjectBean(value.getId(), json.getBytes(StandardCharsets.UTF_8), supportedType);
	}

	@Override
	public ScheduledProcessingRule fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		JsonNode json;
		try
		{
			json = jsonMapper.readTree(new String(blob.getContents(), StandardCharsets.UTF_8));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize scheduled processing rule from JSON", e);
		}
		return new ScheduledProcessingRule((ObjectNode) json, actionsRegistry);
	}

}
