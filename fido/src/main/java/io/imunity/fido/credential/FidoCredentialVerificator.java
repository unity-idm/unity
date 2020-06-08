/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

import static java.util.Objects.isNull;

/**
 * Simple implementation of Fido2 verifier.
 *
 * @author R. Ledzinski
 */
@PrototypeComponent
public class FidoCredentialVerificator extends AbstractLocalVerificator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoCredentialVerificator.class);
	public static final String NAME = "fido";
	public static final String DESC = "Verifies fido credential";

	private CredentialHelper credentialHelper;
	private EntityManagement idMan;

	@Autowired
	public FidoCredentialVerificator(final CredentialHelper credentialHelper)
	{
		super(NAME, DESC, FidoExchange.ID, false);
		this.credentialHelper = credentialHelper;
	}

	@Override
	public String getSerializedConfiguration()
	{
		// TODO return proper configuration when created.
		// JsonUtil.serialize(credential.getSerializedConfiguration());
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		// TODO implementation to be provided
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential, boolean verify)
			throws IllegalCredentialException
	{
		return rawCredential;
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential)
	{
		String credDetails = isNull(currentCredential) ? "" : currentCredential;
		return new CredentialPublicInformation(credDetails.isEmpty() ? LocalCredentialState.notSet : LocalCredentialState.correct, credDetails);
	}

	@Override
	public String invalidate(String currentCredential)
	{
		throw new IllegalStateException("This credential doesn't support invalidation");
	}

	@Override
	public boolean isCredentialSet(EntityParam entity)
			throws EngineException
	{
		return credentialHelper.isCredentialSet(entity, credentialName);
	}

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		return false;
	}

	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<FidoCredentialVerificator> factory)
		{
			super(NAME, DESC, false, factory);
		}
	}

}





