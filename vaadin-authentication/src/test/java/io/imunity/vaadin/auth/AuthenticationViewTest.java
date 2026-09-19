/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class AuthenticationViewTest
{
	@Test
	void shouldInitializeOnlyOnceWhenEnteredRepeatedly()
	{
		// given
		AuthenticationView view = mock(AuthenticationView.class, CALLS_REAL_METHODS);
		doNothing().when(view).init();

		// when
		view.initializeOnce();
		view.initializeOnce();

		// then
		verify(view, times(1)).init();
	}
}
