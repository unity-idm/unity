/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement.UnboundRelayStateException;

public class RemoteAuthenticationContextManagementTest
{
	@Test
	public void shouldPurgeStaleContext() throws InterruptedException
	{
		RemoteAuthenticationContextManagement<TestContext> ctxManager 
			= new RemoteAuthenticationContextManagement<>(Duration.ofMillis(20), Duration.ofMillis(5));
		
		ctxManager.addAuthnContext(new TestContext("c1"));
		Thread.sleep(21);
		
		Throwable error = catchThrowable(() -> ctxManager.getAndRemoveAuthnContext("c1"));
		
		assertThat(error).isInstanceOf(UnboundRelayStateException.class);
	}

	@Test
	public void shouldReturnValidContext() throws InterruptedException
	{
		RemoteAuthenticationContextManagement<TestContext> ctxManager 
			= new RemoteAuthenticationContextManagement<>(Duration.ofMillis(10_000), Duration.ofMillis(5));
		
		TestContext origContext = new TestContext("c1");
		ctxManager.addAuthnContext(origContext);
		
		TestContext authnContext = ctxManager.getAndRemoveAuthnContext("c1");
		
		assertThat(authnContext).isEqualTo(origContext);
	}

	@Test
	public void shouldCleanReturnedContext() throws InterruptedException
	{
		RemoteAuthenticationContextManagement<TestContext> ctxManager 
			= new RemoteAuthenticationContextManagement<>(Duration.ofMillis(10_000), Duration.ofMillis(5));
		
		ctxManager.addAuthnContext(new TestContext("c1"));
		ctxManager.getAndRemoveAuthnContext("c1");
		
		Throwable error = catchThrowable(() -> ctxManager.getAndRemoveAuthnContext("c1"));
		
		assertThat(error).isInstanceOf(UnboundRelayStateException.class);
	}

	
	private static class TestContext extends RelayedAuthnState
	{
		TestContext(String state)
		{
			super(state, new Date());
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(getCreationTime(), getRelayState());
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RelayedAuthnState other = (RelayedAuthnState) obj;
			return Objects.equals(getCreationTime(), other.getCreationTime()) 
					&& Objects.equals(getRelayState(), other.getRelayState());
		}
	}
}
