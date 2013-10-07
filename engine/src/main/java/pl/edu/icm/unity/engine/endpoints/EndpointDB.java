/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoints;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;

/**
 * Boilerplate: easy access to {@link EndpointInstance} storage.
 * <p>
 * Additionally consistency listeners are registered here.
 * 
 * @author K. Benedyczak
 */
@Component
public class EndpointDB extends GenericObjectsDB<EndpointInstance>
{
	@Autowired
	public EndpointDB(EndpointHandler handler, DBGeneric dbGeneric, 
			DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, EndpointInstance.class,
				"endpoint");
		notificationManager.addListener(new AuthenticatorChangeListener());
	}
	
	
	/**
	 * Listens for Authenticator removals. If the authenticator is used by any of endpoints an exception
	 * is thrown.
	 * @author K. Benedyczak
	 */
	private class AuthenticatorChangeListener implements DependencyChangeListener<AuthenticatorInstance>
	{
		@Override
		public String getDependencyObjectType()
		{
			return AuthenticatorInstanceHandler.AUTHENTICATOR_OBJECT_TYPE;
		}

		@Override
		public void preAdd(AuthenticatorInstance newObject, SqlSession sql) throws EngineException { }

		@Override
		public void preUpdate(AuthenticatorInstance oldObject,
				AuthenticatorInstance updatedObject, SqlSession sql) throws EngineException {}

		@Override
		public void preRemove(AuthenticatorInstance removedObject, SqlSession sql)
				throws EngineException
		{
			List<EndpointInstance> endpoints = getAll(sql);
			for (EndpointInstance instance: endpoints)
			{
				List<AuthenticatorSet> used = instance.getEndpointDescription().getAuthenticatorSets();
				for (AuthenticatorSet set: used)
					if (set.getAuthenticators().contains(removedObject.getId()))
						throw new IllegalArgumentException("The authenticator " + 
								removedObject.getId() + 
								" is used by the endpoint " + 
								instance.getEndpointDescription().getId());
			}
		}
	}
}
