/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import io.imunity.console.views.directory_browser.EntityWithLabel;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;

class IdentityEntryFixtures
{
	static Entity entity(long id)
	{
		return new Entity(List.of(new Identity("identifier", "id" + id, id, "id" + id)), new EntityInformation(id),
				new CredentialInfo("credReq", Map.of()));
	}

	static IdentityEntry entityRow(long entityId)
	{
		return new IdentityEntry(new EntityWithLabel(entity(entityId), null), mock(MessageSource.class));
	}
}
