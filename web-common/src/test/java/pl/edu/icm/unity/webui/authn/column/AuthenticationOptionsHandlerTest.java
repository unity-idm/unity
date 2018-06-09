/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

public class AuthenticationOptionsHandlerTest
{
	@Test
	public void shouldReturnRemaining()
	{
		AuthenticationOption opt1 = getMockAuthnOption("authn", "o1", "o2");
		AuthenticationOption opt2 = getMockAuthnOption("authn2", "o3");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(opt1, opt2));
		handler.getMatchingRetrievals("authn.o2");
		
		Map<AuthenticationOption, List<VaadinAuthenticationUI>> result = handler.getRemainingRetrievals();
		
		assertThat(result.size(), is(2));
		assertThat(result.get(opt1).size(), is(1));
		assertThat(result.get(opt1).get(0).getId(), is("o1"));
		assertThat(result.get(opt2).size(), is(1));
		assertThat(result.get(opt2).get(0).getId(), is("o3"));
	}
	
	@Test
	public void shouldBlacklistOptionsGivenByAuthenticator()
	{
		AuthenticationOption opt1 = getMockAuthnOption("authn", "o1", "o2");
		AuthenticationOption opt2 = getMockAuthnOption("authn2", "o3");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(opt1, opt2));
		handler.getMatchingRetrievals("authn");
		
		List<VaadinAuthenticationUI> result = handler.getMatchingRetrievals("authn2");
		
		assertThat(result.size(), is(1));
		assertThat(result.get(0).getId(), is("o3"));
	}

	@Test
	public void shouldIncludeSpecificOption()
	{
		AuthenticationOption opt1 = getMockAuthnOption("authn", "o1", "o2");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(opt1));
		
		List<VaadinAuthenticationUI> result = handler.getMatchingRetrievals("authn.o2");
		
		assertThat(result.size(), is(1));
		assertThat(result.get(0).getId(), is("o2"));
	}
	
	private AuthenticationOption getMockAuthnOption(String authenticator, String... entries)
	{
		VaadinAuthentication vauthenticator = mock(VaadinAuthentication.class);
		when(vauthenticator.getAuthenticatorId()).thenReturn(authenticator);
		List<VaadinAuthenticationUI> uis = new ArrayList<>();
		for (String entry: entries)
		{
			VaadinAuthenticationUI ui = mock(VaadinAuthenticationUI.class);
			when(ui.getId()).thenReturn(entry);
			when(ui.isAvailable()).thenReturn(true);
			uis.add(ui);
		}
		when(vauthenticator.createUIInstance()).thenReturn(uis);
		return new AuthenticationOption(vauthenticator, null);
	}
}
