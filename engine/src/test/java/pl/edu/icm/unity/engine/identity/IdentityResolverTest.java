/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;

public class IdentityResolverTest
{
	@Test
	public void shouldReturnDisplayedNameAttributeIfPresent() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityManagement entityManagement = mock(EntityManagement.class);
		when(entityManagement.getEntityLabel(entity)).thenReturn("dName");
		
		IdentityResolver resolver = new IdentityResolverImpl(null, null, null, null, null, entityManagement);
		
		String displayedUserName = resolver.getDisplayedUserName(entity);
		
		assertThat(displayedUserName).isEqualTo("dName");
	}
	
	@Test
	public void shouldReturnHumanReadableIdentityIfPresent() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityManagement entityManagement = mock(EntityManagement.class);
		when(entityManagement.getEntityLabel(entity)).thenReturn(null);
		Entity resolvedEntity = new Entity(asList(new Identity(X500Identity.ID, "x500", 123l, "x500")), null, null);
		when(entityManagement.getEntity(entity)).thenReturn(resolvedEntity);
		
		IdentityResolver resolver = new IdentityResolverImpl(null, null, null, null, null, entityManagement);
		
		String displayedUserName = resolver.getDisplayedUserName(entity);
		
		assertThat(displayedUserName).isEqualTo("x500");		
	}

	@Test
	public void shouldReturnNullIfNoInfoPresent() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityManagement entityManagement = mock(EntityManagement.class);
		when(entityManagement.getEntityLabel(entity)).thenReturn(null);
		Entity resolvedEntity = new Entity(asList(new Identity(IdentifierIdentity.ID, "xyz", 123l, "xyz")), null, null);
		when(entityManagement.getEntity(entity)).thenReturn(resolvedEntity);
		
		IdentityResolver resolver = new IdentityResolverImpl(null, null, null, null, null, entityManagement);
		
		String displayedUserName = resolver.getDisplayedUserName(entity);
		
		assertThat(displayedUserName).isNull();
	}
}
