/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.sms;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.stdext.credential.CredentialResetBase;
import pl.edu.icm.unity.types.basic.IdentityTaV;
/**
 * SMS reset implementation of {@link CredentialReset}. This implementation is stateful, i.e. from creation it
 * must be used exclusively by a single reset procedure.
 * @author P. Piernik
 */
public class SMSCredentialResetImpl extends CredentialResetBase
{
	private SMSCredentialRecoverySettings settings;
		
	public SMSCredentialResetImpl(NotificationProducer notificationProducer,
			IdentityResolver identityResolver,
			LocalCredentialVerificator localVerificator,
			CredentialHelper credentialHelper,
			String credentialId, 
			ObjectNode completeCredentialConfiguration,
			SMSCredentialRecoverySettings settings)
	{
		super(notificationProducer, identityResolver, localVerificator, credentialHelper, credentialId, completeCredentialConfiguration);
		this.settings = settings;
	}
	
	@Override
	protected String getCredentialSettings()
	{
		ObjectNode node = Constants.MAPPER.createObjectNode();
		settings.serializeTo(node);
		return JsonUtil.toJsonString(node);
	}

	@Override
	protected int getCodeLength()
	{
		return settings.getCodeLength();
	}

	@Override
	public void setSubject(IdentityTaV subject)
	{
		super.setSubject(subject, SMSVerificator.IDENTITY_TYPES);
		
	}
}
