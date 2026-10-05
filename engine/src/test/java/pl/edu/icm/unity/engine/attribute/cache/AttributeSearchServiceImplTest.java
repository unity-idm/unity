/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;
import pl.edu.icm.unity.engine.api.authn.AuthorizationExceptionRT;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;

@ExtendWith(MockitoExtension.class)
public class AttributeSearchServiceImplTest
{
	@Mock
	private AttributesCacheDAO attributesCacheDAO;
	@Mock
	private InternalAuthorizationManager authz;

	private AttributeSearchService create()
	{
		return new AttributeSearchServiceImpl(attributesCacheDAO, authz);
	}

	@Test
	public void shouldReturnMatchesFromDao()
	{
		when(attributesCacheDAO.findEntitiesWithValueContaining("/A", "john")).thenReturn(List.of(1L, 2L));

		Set<Long> result = create().searchEntities("/A", "john");

		assertThat(result).containsExactlyInAnyOrder(1L, 2L);
	}

	@Test
	public void shouldReturnEmptyForBlankTermWithoutQueryingDao()
	{
		AttributeSearchService service = create();

		assertThat(service.searchEntities("/A", "  ")).isEmpty();
		assertThat(service.searchEntities("/A", null)).isEmpty();
		verify(attributesCacheDAO, never()).findEntitiesWithValueContaining(anyString(), anyString());
	}

	@Test
	public void shouldCheckAuthorizationForTheGivenGroup()
	{
		create().searchEntities("/A", "john");

		verify(authz).checkAuthorizationRT("/A", AuthzCapability.readHidden, AuthzCapability.read);
	}

	@Test
	public void shouldPropagateAuthorizationFailureWithoutQueryingDao()
	{
		doThrow(new AuthorizationExceptionRT("denied")).when(authz)
				.checkAuthorizationRT("/A", AuthzCapability.readHidden, AuthzCapability.read);

		AttributeSearchService service = create();

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.searchEntities("/A", "john"))
				.isInstanceOf(AuthorizationExceptionRT.class);
		verify(attributesCacheDAO, never()).findEntitiesWithValueContaining(anyString(), anyString());
	}
}
