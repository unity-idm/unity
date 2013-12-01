/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Adds a previously mapped identity to the local DB, if it is not there. A new entity is created.
 *   
 * @author K. Benedyczak
 */
public class CreateUserAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreateUserAction.class);
	private IdentitiesManagement idsMan;
	private AttributesManagement attrMan;
	private boolean withAttributes;
		
	public CreateUserAction(String[] parameters, IdentitiesManagement idsMan, AttributesManagement attrMan)
	{
		setParameters(parameters);
		this.idsMan = idsMan;
		this.attrMan = attrMan;
	}
	
	@Override
	public String getName()
	{
		return CreateUserActionFactory.NAME;
	}
	
	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		RemoteIdentity ri = input.getPrimaryIdentity();
		if (ri == null)
		{
			log.debug("No identity, skipping");
			return;
		}
		String unityIdentity = ri.getMetadata().get(RemoteInformationBase.UNITY_IDENTITY);
		if (unityIdentity == null)
		{
			log.debug("No mapped identity, skipping");
			return;
		}
		String credReqId = ri.getMetadata().get(RemoteInformationBase.UNITY_IDENTITY_CREDREQ);
		if (credReqId == null)
		{
			log.debug("No credential requirement set for mapped identity, skipping");
			return;
		}
		IdentityParam toAdd = new IdentityParam(ri.getIdentityType(), unityIdentity, false);
		try
		{
			idsMan.getEntity(new EntityParam(toAdd));
			log.debug("Local identity already exists, skipping");
			return;
		} catch (IllegalIdentityValueException e)
		{
			//ok - doesn't exist
		}
		
		if (withAttributes)
		{
			List<Attribute<?>> attributes = getRootGroupAttributes(input);
			log.info("Adding entity " + toAdd + " with attributes to the local DB");
			idsMan.addEntity(toAdd, credReqId, EntityState.valid, false, attributes);
		} else
		{
			log.info("Adding entity " + toAdd + " to the local DB");
			idsMan.addEntity(toAdd, credReqId, EntityState.valid, false);
		}
	}

	private List<Attribute<?>> getRootGroupAttributes(RemotelyAuthenticatedInput input) throws EngineException
	{
		List<Attribute<?>> attrs = AbstractRemoteVerificator.extractAttributes(input, attrMan);
		List<Attribute<?>> ret = new ArrayList<>();
		for (Attribute<?> attr: attrs)
		{
			if (attr.getGroupPath().equals("/"))
				ret.add(attr);
		}
		return ret;
	}
	
	@Override
	public String[] getParameters()
	{
		return new String[] {String.valueOf(withAttributes)};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 1)
			throw new IllegalArgumentException("Action requires exactely 1 parameter");
		withAttributes = Boolean.valueOf(parameters[0]);
	}
}
