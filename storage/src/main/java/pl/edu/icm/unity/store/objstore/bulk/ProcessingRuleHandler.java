/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link ScheduledProcessingRule}
 * 
 * @author K. Benedyczak
 */
@Component
public class ProcessingRuleHandler extends DefaultEntityHandler<ScheduledProcessingRule>
{
	public static final String PROCESSING_RULE_OBJECT_TYPE = "processingRule";

	@Autowired
	public ProcessingRuleHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, PROCESSING_RULE_OBJECT_TYPE, ScheduledProcessingRule.class);
	}

	@Override
	public GenericObjectBean toBlob(ScheduledProcessingRule value)
	{
		try
		{
			return new GenericObjectBean(value.getId(),
					jsonMapper.writeValueAsBytes(ScheduledProcessingRuleMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize scheduled processing rule to JSON", e);

		}
	}

	@Override
	public ScheduledProcessingRule fromBlob(GenericObjectBean blob)
	{
		try
		{
			return ScheduledProcessingRuleMapper
					.map(jsonMapper.readValue(blob.getContents(), DBScheduledProcessingRule.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize scheduled processing rule from JSON", e);
		}
	}
}
