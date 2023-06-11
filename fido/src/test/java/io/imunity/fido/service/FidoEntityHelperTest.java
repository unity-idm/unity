/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import io.imunity.fido.identity.FidoUserHandleIdentity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for {@link FidoEntityHelper}.
 *
 * @author R. Ledzinski
 */
public class FidoEntityHelperTest
{

	private static final String USERNAME = "username";
	private static final String EMAIL = "user@example.com";
	private static final String DISPLAY_NAME = "User display name";
	private static final FidoUserHandle USER_HANDLE = FidoUserHandle.create();
	private static final Identity USERHANDLE_IDENTITY = new Identity(FidoUserHandleIdentity.ID, USER_HANDLE.asString(), 1, USER_HANDLE.asString());
	private static final EntityParam USERHANDLE_IDENTITY_PARAMS = new EntityParam(new IdentityParam(FidoUserHandleIdentity.ID, USERHANDLE_IDENTITY.getValue()));
	private static final EntityParam USERNAME_IDENTITY_PARAMS = new EntityParam(new IdentityParam(UsernameIdentity.ID, USERNAME));
	private static final EntityParam EMAIL_IDENTITY_PARAMS = new EntityParam(new IdentityParam(EmailIdentity.ID, EMAIL));
	private static List<Identity> identitiesList = Arrays.asList(new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME),
				new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL),
				USERHANDLE_IDENTITY);

	@Mock
	private EntityResolver entityResolver;

	@Mock
	private IdentityResolver identityResolver;

	@Mock
	private AttributeSupport attributeSupport;

	@Mock
	private MessageSource msg;

	private FidoEntityHelper helper;

	@Before
	public void setup() throws EngineException
	{
		initMocks(this);
		helper = new FidoEntityHelper(entityResolver, identityResolver, attributeSupport, msg);
		when(identityResolver.getIdentitiesForEntity(eq(USERHANDLE_IDENTITY_PARAMS))).thenReturn(identitiesList);
		when(identityResolver.getIdentitiesForEntity(eq(USERNAME_IDENTITY_PARAMS))).thenReturn(identitiesList);
		when(identityResolver.getIdentitiesForEntity(eq(EMAIL_IDENTITY_PARAMS))).thenReturn(identitiesList);
		when(identityResolver.getIdentitiesForEntity(eq(new EntityParam(1L)))).thenReturn(identitiesList);
	}

	@Test
	public void shouldReturnDisplayName() throws FidoException, EngineException
	{
		//given
		when(attributeSupport.getAttributeValueByMetadata(any(), any(), any())).thenReturn(Optional.of(DISPLAY_NAME));

		//when
		String dn = helper.getDisplayName(Identities.builder().identities(identitiesList).build());

		//then
		assertEquals(DISPLAY_NAME, dn);
	}

	@Test
	public void shouldReturnDefaultDisplayName() throws FidoException, EngineException
	{
		//given
		when(attributeSupport.getAttributeValueByMetadata(any(), any(), any())).thenReturn(Optional.empty());
		when(entityResolver.getEntityId(eq(USERNAME_IDENTITY_PARAMS))).thenReturn(2L);

		//when
		String dn = helper.getDisplayName(Identities.builder().identities(identitiesList).build());

		//then
		assertEquals("Entity [2]", dn);
	}

	@Test
	public void shouldReturnUsernameForEmailIdentity() throws EngineException
	{
		//given
		List<Identity> identitiesList = Collections.singletonList(new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL));
		when(identityResolver.getIdentitiesForEntity(eq(new EntityParam(1L)))).thenReturn(identitiesList);

		//when
		Identities ids = helper.resolveUsername(1L, null).get();

		// then
		assertEquals(EMAIL, ids.getUsername());
	}

	@Test
	public void shouldReturnUsernameForUsernameIdentity() throws EngineException
	{
		//given
		List<Identity> identitiesList = Collections.singletonList(new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME));
		when(identityResolver.getIdentitiesForEntity(eq(new EntityParam(1L)))).thenReturn(identitiesList);

		//when
		Identities ids = helper.resolveUsername(1L, null).get();

		// then
		assertEquals(USERNAME, ids.getUsername());
	}

	@Test
	public void shouldReturnUsernameForTwoIdentity() throws EngineException
	{
		//given/when
		Identities ids = helper.resolveUsername(1L, null).get();

		// then
		assertEquals(USERNAME, ids.getUsername());
	}

	public void shouldReturnEmptyForMissingUsername() throws EngineException
	{
		//given
		List<Identity> identitiesList = Collections.singletonList(USERHANDLE_IDENTITY);
		when(identityResolver.getIdentitiesForEntity(eq(new EntityParam(1L)))).thenReturn(identitiesList);

		//when/then
		assertEquals(Optional.empty(), helper.resolveUsername(1L, null));
	}

	public void shouldReturnEmptyForMissingUsername2() throws EngineException
	{
		//given
		when(identityResolver.getIdentitiesForEntity(eq(new EntityParam(1L)))).thenReturn(Collections.emptyList());

		//when/then
		assertEquals(Optional.empty(), helper.resolveUsername(1L, null));
	}

	@Test
	public void shouldReturnUserHandle() throws EngineException
	{
		//given/when
		Optional<String> uh = helper.getUserHandleForUsername(USERNAME);

		// then
		assertEquals(USERHANDLE_IDENTITY.getValue(), uh.get());
	}

	@Test
	public void shouldReturnEmptyForMissingUserHandle() throws EngineException
	{
		//given
		List<Identity> identitiesList = Collections.singletonList(new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME));
		when(identityResolver.getIdentitiesForEntity(USERNAME_IDENTITY_PARAMS)).thenReturn(identitiesList);

		//when
		Optional<String> uh = helper.getUserHandleForUsername(USERNAME);

		//then
		assertFalse(uh.isPresent());
	}

	@Test
	public void shouldCreateUserHandle() throws EngineException
	{
		//given
		List<Identity> identitiesList = Collections.singletonList(new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME));
		when(identityResolver.getIdentitiesForEntity(eq(new EntityParam(1L)))).thenReturn(identitiesList);
		Identities ids = Identities.builder().identities(Arrays.asList(new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME),
					new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL)))
				.build();

		FidoUserHandle userHandle = FidoUserHandle.create();
		when(identityResolver.insertIdentity(any(), any())).thenReturn(new Identity(FidoUserHandleIdentity.ID, userHandle.asString(), 1, userHandle.asString()));

		//when
		FidoUserHandle uh = helper.getOrCreateUserHandle(ids, userHandle.asString());

		// then
		verify(identityResolver).insertIdentity(any(), any());
		assertEquals(userHandle.asString(), uh.asString());
	}

	@Test
	public void shouldNotCreateUserHandleWhenExists() throws EngineException
	{
		//given
		Identities ids = Identities.builder().identities(identitiesList).build();

		//when
		FidoUserHandle uh = helper.getOrCreateUserHandle(ids);

		// then
		verify(identityResolver, never()).insertIdentity(any(), any());
		assertEquals(USER_HANDLE.asString(), uh.asString());
	}

	@Test
	public void shouldGetUserHandleForUsername() throws EngineException
	{
		//given/when
		Optional<String> uh = helper.getUserHandleForUsername(USERNAME);

		// then
		assertEquals(USERHANDLE_IDENTITY.getValue(), uh.get());
	}

	@Test
	public void shouldGetUsernameForUserHandle() throws EngineException
	{
		//given/when
		Optional<String> un = helper.getUsernameForUserHandle(USERHANDLE_IDENTITY.getValue());

		// then
		assertEquals(USERNAME, un.get());
	}
}
