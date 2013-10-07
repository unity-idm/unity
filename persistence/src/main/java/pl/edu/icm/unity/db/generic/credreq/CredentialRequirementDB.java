/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.credreq;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Easy access to {@link CredentialRequirements} storage.
 * <p>
 * Adds consistency checking: credential can not be removed if used in one of credential requirements.
 * @author K. Benedyczak
 */
@Component
public class CredentialRequirementDB extends GenericObjectsDB<CredentialRequirements>
{
	@Autowired
	public CredentialRequirementDB(CredentialRequirementHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, CredentialRequirements.class,
				"credential requirement");
		notificationManager.addListener(new CredentialChangeListener());
	}
	
	private class CredentialChangeListener implements DependencyChangeListener<CredentialDefinition>
	{
		@Override
		public String getDependencyObjectType()
		{
			return CredentialHandler.CREDENTIAL_OBJECT_TYPE;
		}

		@Override
		public void preAdd(CredentialDefinition newObject, SqlSession sql) throws EngineException { }

		@Override
		public void preUpdate(CredentialDefinition oldObject,
				CredentialDefinition updatedObject, SqlSession sql) throws EngineException 
		{
		}

		@Override
		public void preRemove(CredentialDefinition removedObject, SqlSession sql)
				throws EngineException
		{
			List<CredentialRequirements> crs = getAll(sql);
			for (CredentialRequirements cr: crs)
			{
				if (cr.getRequiredCredentials().contains(removedObject.getName()))
					throw new IllegalCredentialException("The credential is used by a credential requirement " 
							+ cr.getName());
			}
		}
	}

}
