/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import io.imunity.fido.identity.FidoUserHandleIdentity;
import org.junit.Test;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link Identities} class
 */
public class IdentitiesTest
{
	private static final FidoUserHandle USER_HANDLE = FidoUserHandle.create();
	private static final String USERNAME = "username";
	private static final String EMAIL = "user@example.com";
	private static final Identity USERHANDLE_IDENTITY = new Identity(FidoUserHandleIdentity.ID, USER_HANDLE.asString(), 1, USER_HANDLE.asString());
	private static final Identity USERNAME_IDENTITY = new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME);
	private static final Identity EMAIL_IDENTITY = new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL);

	@Test(expected = IllegalArgumentException.class)
	public void identitiesBuilderFailWhenUsernameAndEmailIdentitiesAreMissing() {
		//given
		List<Identity> identities = Arrays.asList(USERHANDLE_IDENTITY);
		//when/then
		Identities.builder().identities(identities).build();
	}

	@Test
	public void getUsernameReturnEmailWhenOnlyEmailIdentityIsPresent() {
		//given
		List<Identity> identities = Arrays.asList(USERHANDLE_IDENTITY, EMAIL_IDENTITY);

		//when
		String username = Identities.builder().identities(identities).build().getUsername();

		//then
		assertEquals(EMAIL, username);
	}

	@Test
	public void getUsernameReturnUsernameWhenBothUsernameAndEmailIdentityIsPresent() {
		//given
		List<Identity> identities = Arrays.asList(USERHANDLE_IDENTITY, EMAIL_IDENTITY, USERNAME_IDENTITY);
		List<Identity> reversedIdentities = Arrays.asList(USERNAME_IDENTITY, EMAIL_IDENTITY, USERHANDLE_IDENTITY);

		//when
		String username1 = Identities.builder().identities(identities).build().getUsername();
		String username2 = Identities.builder().identities(reversedIdentities).build().getUsername();

		//then
		assertEquals(USERNAME, username1);
		assertEquals(USERNAME, username2);
	}
}
