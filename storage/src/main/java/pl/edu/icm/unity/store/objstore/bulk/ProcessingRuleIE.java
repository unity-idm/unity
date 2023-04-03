/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;

/**
 * Handles import/export of {@link ScheduledProcessingRule}.
 * 
 * @author K. Benedyczak
 */
@Component
public class ProcessingRuleIE extends GenericObjectIEBase<ScheduledProcessingRule>
{
	@Autowired
	public ProcessingRuleIE(ProcessingRuleDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 110, ProcessingRuleHandler.PROCESSING_RULE_OBJECT_TYPE);
	}

	@Override
	protected ScheduledProcessingRule convert(ObjectNode src)
	{
		return ScheduledProcessingRuleMapper.map(jsonMapper.convertValue(src, DBScheduledProcessingRule.class));
	}

	@Override
	protected ObjectNode convert(ScheduledProcessingRule src)
	{
		return jsonMapper.convertValue(ScheduledProcessingRuleMapper.map(src), ObjectNode.class);
	}
}
