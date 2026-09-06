/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributesCacheInvalidation;

/**
 * Translates {@link EntityAttributesChangedEvent}s into attributes cache invalidation. Spring's default
 * event multicaster dispatches synchronously and on the publishing thread, so this still runs within the
 * originating transaction, satisfying the requirement that the pending flag is set synchronously.
 */
@Component
class AttributesCacheInvalidationListener
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributesCacheInvalidationListener.class);

	private final AttributesCacheInvalidation attributesCacheInvalidation;

	@Autowired
	AttributesCacheInvalidationListener(AttributesCacheInvalidation attributesCacheInvalidation)
	{
		this.attributesCacheInvalidation = attributesCacheInvalidation;
	}

	@EventListener
	public void onEntityAttributesChanged(EntityAttributesChangedEvent event)
	{
		log.trace("Received EntityAttributesChangedEvent for entity {}", event.entityId());
		attributesCacheInvalidation.invalidateEntity(event.entityId());
	}
}
