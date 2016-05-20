/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;

/**
 * Handles import/export of {@link ScheduledProcessingRule}.
 * @author K. Benedyczak
 */
@Component
public class ProcessingRuleIE extends GenericObjectIEBase<ScheduledProcessingRule>
{
	@Autowired
	public ProcessingRuleIE(ProcessingRuleDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, ScheduledProcessingRule.class, 110, 
				ProcessingRuleHandler.PROCESSING_RULE_OBJECT_TYPE);
	}
}



