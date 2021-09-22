/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class IdentityResolverTest
{
	@Test
	public void shouldReturnDisplayedNameAttributeIfPresent() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityManagement entityManagement = mock(EntityManagement.class);
		when(entityManagement.getEntityLabel(entity)).thenReturn("dName");
		
		IdentityResolver resolver = new IdentityResolverImpl(null, null, null, null, null, null, entityManagement);
		
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
		
		IdentityResolver resolver = new IdentityResolverImpl(null, null, null, null, null, null, entityManagement);
		
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
		
		IdentityResolver resolver = new IdentityResolverImpl(null, null, null, null, null, null, entityManagement);
		
		String displayedUserName = resolver.getDisplayedUserName(entity);
		
		assertThat(displayedUserName).isNull();
	}
	
	@Test
	public void shouldResolveUserNameIdentityFromAuthenticationSubjectEntityId() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityDAO dbIdentities = mock(EntityDAO.class);
		IdentityHelper idHelper = mock(IdentityHelper.class);
		EntityResolver dbResolver = mock(EntityResolver.class);
		when(dbIdentities.getByKey(123l)).thenReturn(new EntityInformation(123l));
		when(dbResolver.getEntityId(entity)).thenReturn(123l);
		when(idHelper.getIdentitiesForEntity(123l, null))
				.thenReturn(new ArrayList<Identity>(Arrays.asList(new Identity(UsernameIdentity.ID, "id", 123l, ""))));

		IdentityResolver resolver = new IdentityResolverImpl(null, dbIdentities, dbResolver, null, null, idHelper,
				null);

		Identity id = resolver.resolveSubject(AuthenticationSubject.entityBased(123l), UsernameIdentity.ID);

		assertThat(id.getValue(), is("id"));
	}
	
	@Test
	public void shouldResolveUserNameIdentityFromAuthenticationSubjectIdentity() throws EngineException
	{
		EntityDAO dbIdentities = mock(EntityDAO.class);
		EntityResolver dbResolver = mock(EntityResolver.class);
		when(dbIdentities.getByKey(123l)).thenReturn(new EntityInformation(123l));
		when(dbResolver.getFullIdentity(new IdentityTaV(UsernameIdentity.ID, "id")))
				.thenReturn(new Identity(UsernameIdentity.ID, "id", 123l, ""));

		IdentityResolver resolver = new IdentityResolverImpl(null, dbIdentities, dbResolver, null, null, null, null);
		Identity id = resolver.resolveSubject(AuthenticationSubject.identityBased("id"), UsernameIdentity.ID);
		assertThat(id.getValue(), is("id"));
	}
	
	@Test
	public void shouldThrowExceptionWhenNoIdentityForSubject() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityDAO dbIdentities = mock(EntityDAO.class);
		IdentityHelper idHelper = mock(IdentityHelper.class);
		EntityResolver dbResolver = mock(EntityResolver.class);
		when(dbIdentities.getByKey(123l)).thenReturn(new EntityInformation(123l));
		when(dbResolver.getEntityId(entity)).thenReturn(123l);
		when(idHelper.getIdentitiesForEntity(123l, null))
				.thenReturn(new ArrayList<Identity>(Arrays.asList(new Identity(EmailIdentity.ID, "id", 123l, ""))));

		IdentityResolver resolver = new IdentityResolverImpl(null, dbIdentities, dbResolver, null, null, idHelper,
				null);

		Throwable error = catchThrowable(
				() -> resolver.resolveSubject(AuthenticationSubject.entityBased(123l), UsernameIdentity.ID));
		assertThat(error).isInstanceOf(IllegalIdentityValueException.class);
	}

	@Test
	public void shouldThrowExceptionWhenMoreThanOneIdentityForSubject() throws EngineException
	{
		EntityParam entity = new EntityParam(123l);
		EntityDAO dbIdentities = mock(EntityDAO.class);
		IdentityHelper idHelper = mock(IdentityHelper.class);
		EntityResolver dbResolver = mock(EntityResolver.class);
		when(dbIdentities.getByKey(123l)).thenReturn(new EntityInformation(123l));
		when(dbResolver.getEntityId(entity)).thenReturn(123l);
		when(idHelper.getIdentitiesForEntity(123l, null))
				.thenReturn(new ArrayList<Identity>(Arrays.asList(new Identity(UsernameIdentity.ID, "id", 123l, ""),
						new Identity(UsernameIdentity.ID, "id2", 123l, ""))));

		IdentityResolver resolver = new IdentityResolverImpl(null, dbIdentities, dbResolver, null, null, idHelper,
				null);

		Throwable error = catchThrowable(
				() -> resolver.resolveSubject(AuthenticationSubject.entityBased(123l), UsernameIdentity.ID));
		assertThat(error).isInstanceOf(IllegalIdentityValueException.class);
	}
}
