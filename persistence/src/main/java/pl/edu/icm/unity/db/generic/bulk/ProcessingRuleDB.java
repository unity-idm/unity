/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.bulk;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;

/**
 * Easy to use interface to {@link AbstractTranslationProfile} storage.
 * @author K. Benedyczak
 */
@Component
public class ProcessingRuleDB extends GenericObjectsDB<ScheduledProcessingRule>
{

	public ProcessingRuleDB(ProcessingRuleHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, ScheduledProcessingRule.class, "processing rule");
	}
}
