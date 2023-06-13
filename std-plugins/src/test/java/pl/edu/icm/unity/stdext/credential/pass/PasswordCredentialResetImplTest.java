/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.Test;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;

public class PasswordCredentialResetImplTest
{
	private AuthenticationSubject subject = AuthenticationSubject.identityBased("identity");
	private NotificationProducer notificationProducer;
	private IdentityResolver identityResolver;

	@Test
	public void shouldAcceptCorrectCode() throws EngineException
	{
		initMocks();
		PasswordCredentialResetImpl resetImpl = new PasswordCredentialResetImpl(notificationProducer, 
				identityResolver, 
				null, 
				null, 
				"credentialId", 
				null, 
				mock(PasswordCredentialResetSettings.class), 
				null); 
		resetImpl.setSubject(subject);
		resetImpl.sendCode("msgTemplate", false);
		
		String sentCode = resetImpl.getSentCode();
		assertThat(sentCode).isNotNull();
		
		resetImpl.verifyDynamicData(sentCode);
	}
	
	@Test
	public void shouldNotAcceptIncorrectCode() throws EngineException
	{
		initMocks();
		PasswordCredentialResetImpl resetImpl = new PasswordCredentialResetImpl(notificationProducer, 
				identityResolver, 
				null, 
				null, 
				"credentialId", 
				null, 
				mock(PasswordCredentialResetSettings.class), 
				null); 
		resetImpl.setSubject(subject);
		resetImpl.sendCode("msgTemplate", false);
		
		String sentCode = resetImpl.getSentCode();
		assertThat(sentCode).isNotNull();
		
		Throwable error = catchThrowable(() -> resetImpl.verifyDynamicData(sentCode + "foo"));
		
		assertThat(error).isNotNull().isInstanceOf(WrongArgumentException.class);
	}

	@Test
	public void shouldNotAcceptExpiredCode() throws EngineException, InterruptedException
	{
		initMocks();
		PasswordCredentialResetImpl resetImpl = new PasswordCredentialResetImpl(notificationProducer, 
				identityResolver, 
				null, 
				null, 
				"credentialId", 
				null, 
				mock(PasswordCredentialResetSettings.class), 
				null, 
				Duration.ofMillis(10)); 
		resetImpl.setSubject(subject);
		resetImpl.sendCode("msgTemplate", false);
		
		String sentCode = resetImpl.getSentCode();
		assertThat(sentCode).isNotNull();
		
		Thread.sleep(11);
		
		Throwable error = catchThrowable(() -> resetImpl.verifyDynamicData(sentCode));
		
		assertThat(error).isNotNull().isInstanceOf(TooManyAttempts.class);
	}

	private void initMocks() throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException,
			EngineException
	{
		notificationProducer = mock(NotificationProducer.class);
		identityResolver = mock(IdentityResolver.class);
		when(identityResolver.resolveSubject(eq(subject), any(), eq("credentialId"))).thenReturn(
				new EntityWithCredential("credentialId", "credVal", 123));
	}

}

