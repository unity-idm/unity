/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.bulk;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.objstore.AbstractObjStoreTest;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.types.translation.TranslationAction;

public class ProcessingRuleTest extends AbstractObjStoreTest<ScheduledProcessingRule>
{
	@Autowired
	private ProcessingRuleDB dao;
	
	@Override
	protected GenericObjectsDAO<ScheduledProcessingRule> getDAO()
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

	@Override
	protected void assertAreEqual(ScheduledProcessingRule obj, ScheduledProcessingRule cmp)
	{
		assertThat(obj, is(cmp));
	}
}
