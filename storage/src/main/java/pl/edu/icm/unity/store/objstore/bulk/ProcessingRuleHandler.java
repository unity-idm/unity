/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;

/**
 * Handler for {@link ScheduledProcessingRule}
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
		return new GenericObjectBean(value.getId(), JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public ScheduledProcessingRule fromBlob(GenericObjectBean blob)
	{
		return new ScheduledProcessingRule(JsonUtil.parse(blob.getContents()));
	}
}
