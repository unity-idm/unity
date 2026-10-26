/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.attributes.AttributesCacheInvalidation;

@ExtendWith(MockitoExtension.class)
public class AttributesCacheInvalidationListenerTest
{
	@Mock
	private AttributesCacheInvalidation invalidation;

	@Test
	public void entityChangeEventInvalidatesThatEntity()
	{
		new AttributesCacheInvalidationListener(invalidation).onEntityAttributesChanged(
				new EntityAttributesChangedEvent(7L));

		verify(invalidation).invalidateEntity(7L);
		verifyNoMoreInteractions(invalidation);
	}
}
