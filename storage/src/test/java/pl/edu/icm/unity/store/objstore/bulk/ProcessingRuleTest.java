/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

public class ProcessingRuleTest extends AbstractNamedWithTSTest<ScheduledProcessingRule>
{
	@Autowired
	private ProcessingRuleDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<ScheduledProcessingRule> getDAO()
	{
		return dao;
	}

	@Override
	protected ScheduledProcessingRule getObject(String id)
	{
		TranslationAction action = new TranslationAction("action", new String[] {"p1", "p2"});
		return new ScheduledProcessingRule("condition", action, "cronExpression", id);
	}

	@Override
	protected ScheduledProcessingRule mutateObject(ScheduledProcessingRule ret)
	{
		TranslationAction action = new TranslationAction("action2", new String[] {"p3"});
		return new ScheduledProcessingRule("condition2", action, "cronExpression2", "modified-id");
	}
}
