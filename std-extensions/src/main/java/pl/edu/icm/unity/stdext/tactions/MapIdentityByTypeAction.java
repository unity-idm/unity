/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps an identity of a given type (param1) to unity identity of another type (param2), 
 * preserving the value with a specified credential requirement (useful when entity is created based on the identity).
 *   
 * @author K. Benedyczak
 */
public class MapIdentityByTypeAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapIdentityByTypeAction.class);
	private String sourceIdentityType;
	private String targetIdentityType;
	private String credReq;

	public MapIdentityByTypeAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return MapIdentityByTypeActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		Map<String, RemoteIdentity> identities = input.getIdentities();
		
		boolean primarySet = false;
		for (RemoteIdentity entry: identities.values())
		{
			if (entry.getIdentityType().equals(sourceIdentityType))
			{
				entry.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY, entry.getName());
				entry.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY_TYPE, targetIdentityType);
				entry.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY_CREDREQ, credReq);
				log.debug("Mapped identity of type " + sourceIdentityType + " to " +  
						targetIdentityType + " type, keeping the value " + entry.getName());
				if (!primarySet)
				{
					log.debug("Setting primary identity to " + entry.getName());
					primarySet = true;
					input.setPrimaryIdentityName(entry.getName());
				}
			}
		}
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {sourceIdentityType, targetIdentityType, credReq};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactely 3 parameters");
		sourceIdentityType = parameters[0];
		targetIdentityType = parameters[1];
		credReq = parameters[2];
	}
}
