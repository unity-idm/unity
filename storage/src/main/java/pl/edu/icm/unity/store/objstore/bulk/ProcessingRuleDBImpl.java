/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy access to {@link ScheduledProcessingRule} storage.
 * 
 * @author K. Benedyczak
 */
@Component
public class ProcessingRuleDBImpl extends GenericObjectsDAOImpl<ScheduledProcessingRule> 
		implements ProcessingRuleDB
{
	@Autowired
	public ProcessingRuleDBImpl(ProcessingRuleHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, ScheduledProcessingRule.class, "processing rule");
	}
}
