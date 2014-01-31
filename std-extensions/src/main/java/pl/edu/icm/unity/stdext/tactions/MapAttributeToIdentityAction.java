/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps a specified attribute (param1) to an identity of a specified type (param2) with a specified 
 * credential requirement (useful when entity is created based on the identity).
 *   
 * @author K. Benedyczak
 */
public class MapAttributeToIdentityAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapAttributeToIdentityAction.class);
	private String attribute;
	private String identityType;
	private String credReq;

	public MapAttributeToIdentityAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return MapAttributeToIdentityActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		Map<String, RemoteAttribute> attributes = input.getAttributes();
		
		RemoteAttribute toBeMapped = attributes.get(attribute);
		if (toBeMapped == null)
		{
			log.trace("Attribute " + attribute + " doesn't match");
			return;
		}
		if (toBeMapped.getValues().size() != 1)
		{
			log.debug("Attribute " + attribute + " has " + toBeMapped.getValues().size() + 
					" values so won't be mapped to identity name.");
			return;
		}
		String value = toBeMapped.getValues().get(0).toString();
		
		RemoteIdentity newId = new RemoteIdentity(value, identityType);
		newId.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY, value);
		newId.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY_TYPE, identityType);
		newId.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY_CREDREQ, credReq);
		log.debug("Created a new identity from attribute " + attribute + ": " + value + 
				" of " + identityType + " type");
		input.getIdentities().put(value, newId);
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {attribute, identityType, credReq};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactely 3 parameters");
		attribute = parameters[0];
		identityType = parameters[1];
		credReq = parameters[2];
	}
}
