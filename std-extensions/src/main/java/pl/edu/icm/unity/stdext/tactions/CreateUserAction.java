/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Adds a previously mapped identity to the local DB. If any of the mapped identities exists in the DB
 * only those not present in DB are added and are added as equivalent of the existing entity. 
 * If at least two of the mapped identities exist in DB and are assigned to different entities an error is logged
 * and processing is skipped. 
 * If no mapped entity is in DB a new entity is created with all mapped identities. 
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
		Collection<RemoteIdentity> identities = input.getIdentities().values();
		Collection<IdentityParamWithCR> mappedIdentities = new ArrayList<>();
		for (RemoteIdentity ri: identities)
		{
			Map<String, String> meta = ri.getMetadata();
			if (meta.containsKey(RemoteInformationBase.UNITY_IDENTITY) && 
					meta.containsKey(RemoteInformationBase.UNITY_IDENTITY_TYPE) &&
					meta.containsKey(RemoteInformationBase.UNITY_IDENTITY_CREDREQ))
			{
				mappedIdentities.add(new IdentityParamWithCR(new IdentityParam(
						meta.get(RemoteInformationBase.UNITY_IDENTITY_TYPE), 
						meta.get(RemoteInformationBase.UNITY_IDENTITY), false), 
						meta.get(RemoteInformationBase.UNITY_IDENTITY_CREDREQ)));
			}
		}
		
		Collection<IdentityParamWithCR> mappedMissingIdentities = new ArrayList<>();
		Entity existing = null;
		for (IdentityParamWithCR checked: mappedIdentities)
		{
			try
			{
				Entity found = idsMan.getEntity(new EntityParam(checked.idParam));
				if (existing != null && existing.getId() != found.getId())
				{
					log.info("Identity was mapped to two different entities: " + 
							existing + " and " + found + ". Skipping.");
					return;
				}
				existing = found;
			} catch (IllegalIdentityValueException e)
			{
				mappedMissingIdentities.add(checked);
			}			
		}
		
		if (mappedMissingIdentities.size() == 0)
		{
			log.debug("No identity needs to be added");
			return;
		}
		
		if (existing != null)
		{
			addEquivalents(mappedMissingIdentities.iterator(), new EntityParam(existing.getId()));
		} else
		{
			createNewEntity(input, mappedMissingIdentities);
		}
	}

	private void addEquivalents(Iterator<IdentityParamWithCR> toAdd, EntityParam parentEntity) 
			throws EngineException
	{
		while (toAdd.hasNext())
		{
			idsMan.addIdentity(toAdd.next().idParam, parentEntity, false);
		}
	}
	
	private void createNewEntity(RemotelyAuthenticatedInput input,
			Collection<IdentityParamWithCR> mappedMissingIdentities) throws EngineException
	{
		Iterator<IdentityParamWithCR> toAdd = mappedMissingIdentities.iterator();
		IdentityParamWithCR first = toAdd.next();
		
		Identity added;
		if (withAttributes)
		{
			List<Attribute<?>> attributes = getRootGroupAttributes(input);
			log.info("Adding entity " + first.idParam + " with attributes to the local DB");
			added = idsMan.addEntity(first.idParam, first.credReq, EntityState.valid, false, attributes);
		} else
		{
			log.info("Adding entity " + first.idParam + " to the local DB");
			added = idsMan.addEntity(first.idParam, first.credReq, EntityState.valid, false);
		}
		
		addEquivalents(toAdd, new EntityParam(added));
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
	
	private static class IdentityParamWithCR
	{
		private IdentityParam idParam;
		private String credReq;
		public IdentityParamWithCR(IdentityParam idParam, String credReq)
		{
			this.idParam = idParam;
			this.credReq = credReq;
		}
	}
}
