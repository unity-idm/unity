/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Composite password reset implementation of {@link CredentialReset}. This
 * implementation is only wrapper for {@link PasswordCredentialResetImpl}.
 * Credential to reset is dynamically set only when username is known
 * 
 * @author P.Piernik
 *
 */
public class CompositePasswordResetImpl  implements CredentialReset
{
	private PasswordCredentialResetImpl resetBackend;
	private CredentialHelper credentialHelper;
	private List<LocalCredentialVerificator> localVerificators;
	private IdentityResolver identityResolver;
	private NotificationProducer notificationProducer;
	
	
	public CompositePasswordResetImpl(CredentialHelper credentialHelper,
			List<LocalCredentialVerificator> localVerificators,
			IdentityResolver identityResolver,
			NotificationProducer notificationProducer)
	{
		
		this.credentialHelper = credentialHelper;
		this.localVerificators = localVerificators;
		this.identityResolver = identityResolver;
		this.notificationProducer = notificationProducer;
	}
	
	@Override
	public String getSettings()
	{
		
		if (resetBackend == null)
		{
			//return settings only for information that reset is possible
			PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings();
			settings.setRequireSecurityQuestion(true);
			settings.setQuestions(Arrays.asList(""));
			settings.setEnabled(true);
			ObjectNode node = Constants.MAPPER.createObjectNode();
			settings.serializeTo(node);
			return JsonUtil.toJsonString(node);
		}
	
		return resetBackend.getSettings();
	}

	@Override
	public void setSubject(IdentityTaV subject)
	{

		Optional<EntityWithCredential> resolveIdentity = CompositePasswordHelper
				.getLocalEntity(identityResolver, subject.getValue());

		if (resolveIdentity.isPresent())
		{
			for (LocalCredentialVerificator localVerificator : localVerificators)
			{

				boolean isCredSet = CompositePasswordHelper.checkIfUserHasCredential(
						localVerificator,
						resolveIdentity.get().getEntityId());
				if (!isCredSet)
					continue;

				resetBackend = getResetBackend(localVerificator);
			}

			if (resetBackend == null)
			{
				// we have user but without any credentials
				LocalCredentialVerificator firstVerificator = localVerificators
						.iterator().next();
				resetBackend = getResetBackend(firstVerificator);
			}

		} else
		{
			// we have unknown user, set first possible backend
			LocalCredentialVerificator firstVerificator = localVerificators.iterator()
					.next();
			resetBackend = getResetBackend(firstVerificator);
		}

		resetBackend.setSubject(subject);
	}
	
	private PasswordCredentialResetImpl getResetBackend(LocalCredentialVerificator verificator)
	{
		Optional<CredentialDefinition> credDef = CompositePasswordHelper
				.getCredentialDefinition(credentialHelper,
						verificator.getCredentialName());
		if (!credDef.isPresent())
		{
			throw new InternalException(
					"Invalid configuration of the verificator, local credential "
							+ verificator.getCredentialName()
							+ " is undefined");
		}

		PasswordCredential passwordCredential = new PasswordCredential();
		passwordCredential.setSerializedConfiguration(
				JsonUtil.parse(credDef.get().getConfiguration()));

		return new PasswordCredentialResetImpl(notificationProducer, identityResolver,
				verificator, credentialHelper, verificator.getCredentialName(),
				passwordCredential.getSerializedConfiguration(),
				passwordCredential.getPasswordResetSettings());
	}

	@Override
	public String getSecurityQuestion()
	{
		return resetBackend.getSecurityQuestion();
	}

	@Override
	public void verifyStaticData(String answer) throws WrongArgumentException,
			IllegalIdentityValueException, TooManyAttempts
	{
		 resetBackend.verifyStaticData(answer);
		
	}

	@Override
	public void verifyDynamicData(String emailCode)
			throws WrongArgumentException, TooManyAttempts
	{
		 resetBackend.verifyDynamicData(emailCode);
		
		
	}

	@Override
	public void sendCode(String messageTemplateId, boolean onlyNumberCode)
			throws EngineException
	{
		 resetBackend.sendCode(messageTemplateId, onlyNumberCode);
		
	}

	@Override
	public String getCredentialConfiguration()
	{
		return resetBackend.getCredentialConfiguration();
	}

	@Override
	public void updateCredential(String newCredential) throws EngineException
	{
		resetBackend.updateCredential(newCredential);
		
	}

	@Override
	public Long getEntityId()
	{
		return resetBackend.getEntityId();
	}

}
