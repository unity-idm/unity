/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.events.EventProcessor;
import pl.edu.icm.unity.store.api.tx.TxManager;

/**
 * Used to publish log events from various parts of the app
 *
 * @author R. Ledzinski
 */
@Component
public class AuditPublisher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUDIT, AuditPublisher.class);
	private static final AuditEntity SYSTEM_ENTITY = new AuditEntity(0L, "System", null);

	private EventProcessor eventProcessor;
	private TxManager txMan;
	volatile boolean enabled;

	@Autowired
	public AuditPublisher(final EventProcessor eventProcessor, final TxManager txMan,
						  final UnityServerConfiguration mainConfig)
	{
		this.eventProcessor = eventProcessor;
		this.txMan = txMan;
	}

	public void log(final AuditEventTrigger.AuditEventTriggerBuilder triggerBuilder) 
	{
		if (!enabled)
			return;

		if (InvocationContext.hasCurrent() &&
				InvocationContext.getCurrent().getLoginSession() != null)
		{
			triggerBuilder.initiator(InvocationContext.getCurrent().getLoginSession().getEntityId());
		} else
		{
			log.debug("Missing data in InvocationContext - using System initiator");
			triggerBuilder.initiator(SYSTEM_ENTITY);
		}


		txMan.addPostCommitAction(() -> eventProcessor.fireEvent(triggerBuilder.build()));
	}
}
