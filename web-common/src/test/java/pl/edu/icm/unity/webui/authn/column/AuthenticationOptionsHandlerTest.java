/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

public class AuthenticationOptionsHandlerTest
{
	@Test
	public void shouldReturnRemaining()
	{
		AuthenticationFlow flow1 = getMockAuthnOption("authn", "o1", "o2");
		AuthenticationFlow flow2 = getMockAuthnOption("authn2", "o3");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(flow1, flow2), "endp");
		handler.getMatchingAuthnOptions("authn.o2");
		
		List<AuthNOption> result = handler.getRemainingAuthnOptions();
		
		assertThat(result.size(), is(2));
		assertThat(result.get(0).authenticatorUI.getId(), is("o1"));
		assertThat(result.get(1).authenticatorUI.getId(), is("o3"));
	}
	
	@Test
	public void shouldReturnOptionFromMFAByAuthenticator()
	{
		AuthenticationFlow flow1 = getMock2FAuthnOption("authn1", "authn2", "2ndFAo", "o1", "o2");
		AuthenticationFlow flow2 = getMockAuthnOption("authn3", "o3");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(flow1, flow2), "endp");
		
		List<AuthNOption> result = handler.getMatchingAuthnOptions("authn1");
		
		assertThat(result.size(), is(2));
		assertThat(result.get(0).authenticatorUI.getId(), is("o1"));
		assertThat(result.get(1).authenticatorUI.getId(), is("o2"));
	}

	@Test
	public void shouldReturnOptionFromMFAByEntry()
	{
		AuthenticationFlow flow1 = getMock2FAuthnOption("authn1", "authn2", "2ndFAo", "o1", "o2");
		AuthenticationFlow flow2 = getMockAuthnOption("authn3", "o3");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(flow1, flow2), "endp");
		
		List<AuthNOption> result = handler.getMatchingAuthnOptions("authn1.o2");
		
		assertThat(result.size(), is(1));
		assertThat(result.get(0).authenticatorUI.getId(), is("o2"));
	}
	
	@Test
	public void shouldBlacklistOptionsGivenByAuthenticator()
	{
		AuthenticationFlow flow1 = getMockAuthnOption("authn", "o1", "o2");
		AuthenticationFlow flow2 = getMockAuthnOption("authn2", "o3");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(flow1, flow2), "endp");
		handler.getMatchingAuthnOptions("authn");
		
		List<AuthNOption> result = handler.getMatchingAuthnOptions("authn2");
		
		assertThat(result.size(), is(1));
		assertThat(result.get(0).authenticatorUI.getId(), is("o3"));
	}

	@Test
	public void shouldIncludeSpecificOption()
	{
		AuthenticationFlow flow1 = getMockAuthnOption("authn", "o1", "o2");
		AuthenticationOptionsHandler handler = new AuthenticationOptionsHandler(Lists.newArrayList(flow1), "endp");
		
		List<AuthNOption> result = handler.getMatchingAuthnOptions("authn.o2");
		
		assertThat(result.size(), is(1));
		assertThat(result.get(0).authenticatorUI.getId(), is("o2"));
	}
	
	private AuthenticationFlow getMock2FAuthnOption(String authenticator, 
			String authenticator2, String secondFAEntry, String... entries)
	{
		AuthenticatorInstance vauthenticator1 = getMockVaadinAuthentication(authenticator, entries);
		AuthenticatorInstance vauthenticator2 = getMockVaadinAuthentication(authenticator2, secondFAEntry);
		return new AuthenticationFlow("", Policy.REQUIRE, Sets.newHashSet(vauthenticator1), 
				Lists.newArrayList(vauthenticator2), 1);
	}
	
	private AuthenticationFlow getMockAuthnOption(String authenticator, String... entries)
	{
		return new AuthenticationFlow("", Policy.NEVER, Sets.newHashSet(
				getMockVaadinAuthentication(authenticator, entries)), Lists.newArrayList(), 1);
	}
	
	private AuthenticatorInstance getMockVaadinAuthentication(String authenticator, String... entries)
	{
		VaadinCredRet vauthenticator = mock(VaadinCredRet.class);
		when(vauthenticator.getAuthenticatorId()).thenReturn(authenticator);
		List<VaadinAuthenticationUI> uis = new ArrayList<>();
		for (String entry: entries)
		{
			VaadinAuthenticationUI ui = mock(VaadinAuthenticationUI.class);
			when(ui.getId()).thenReturn(entry);
			when(ui.isAvailable()).thenReturn(true);
			uis.add(ui);
		}
		when(vauthenticator.createUIInstance(Context.LOGIN)).thenReturn(uis);
		AuthenticatorInstance ret = mock(AuthenticatorInstance.class);
		when(ret.getRetrieval()).thenReturn(vauthenticator);
		return ret;
	}
	
	private interface VaadinCredRet extends VaadinAuthentication, CredentialRetrieval
	{
	}

}
