/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.stdext.credential.CredentialResetBase;

public class OTPCredentialReset extends CredentialResetBase
{
	private OTPResetSettings resetSettings;

	public OTPCredentialReset(NotificationProducer notificationProducer, IdentityResolver identityResolver,
			LocalCredentialVerificator localVerificator, CredentialHelper credentialHelper,
			String credentialId, ObjectNode completeCredentialConfiguration,
			OTPResetSettings resetSettings)
	{
		super(notificationProducer, identityResolver, localVerificator, credentialHelper, credentialId,
				completeCredentialConfiguration, CredentialResetBase.DEFAULT_MAX_CODE_VALIDITY);
		this.resetSettings = resetSettings;
	}

	@Override
	public void setSubject(AuthenticationSubject subject)
	{
		super.setSubject(subject, OTPVerificator.IDENTITY_TYPES);
	}

	@Override
	protected String getCredentialSettings()
	{
		return JsonUtil.toJsonString(resetSettings);
	}

	@Override
	protected int getCodeLength()
	{
		return resetSettings.codeLength;
	}

	public OTPResetSettings getResetSettings()
	{
		return resetSettings;
	}
}
