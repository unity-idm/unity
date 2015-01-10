/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.IdentityFromRegState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registrations.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

/**
 * Identity from registration confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class IdentityFromRegistrationRequestFacility extends
		AttributeFromRegistrationRequestFacility implements ConfirmationFacility
{

	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public IdentityFromRegistrationRequestFacility(DBSessionManager db,
			RegistrationRequestDB requestDB, RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment,
			IdentityTypesRegistry identityTypesRegistry)
	{
		super(db, requestDB, formsDB, internalRegistrationManagment);
		this.identityTypesRegistry = identityTypesRegistry;
	}

	@Override
	public String getName()
	{
		return IdentityFromRegState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity from registration request";
	}

	protected ConfirmationStatus confirmElements(RegistrationRequest req, String state)
			throws EngineException
	{
		IdentityFromRegState idState = new IdentityFromRegState();
		idState.setSerializedConfiguration(state);
		if (!identityTypesRegistry.getByName(idState.getType()).isVerifiable())
			return new ConfirmationStatus(false, "ConfirmationStatus.identityChanged");

		Collection<IdentityParam> confirmedList = confirmIdentity(identityTypesRegistry,
				req.getIdentities(), idState.getType(), idState.getValue());

		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed,
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged");

	}

}
