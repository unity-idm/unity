/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authn.AuthenticatorImpl;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.engine.internal.InternalEndpointManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
public class AuthenticationManagementImpl implements AuthenticationManagement
{
	public static final String AUTHENTICATOR_OBJECT_TYPE = "authenticator";
	public static final String CREDENTIAL_REQ_OBJECT_TYPE = "credentialRequirement";
	private AuthenticatorsRegistry authReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private InternalEndpointManagement internalEndpointManagement;
	private IdentityResolver identityResolver;
	private EngineHelper engineHelper;
	
	@Autowired
	public AuthenticationManagementImpl(AuthenticatorsRegistry authReg, DBSessionManager db,
			DBGeneric dbGeneric, InternalEndpointManagement internalEndpointManagement,
			IdentityResolver identityResolver, EngineHelper engineHelper)
	{
		this.authReg = authReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.internalEndpointManagement = internalEndpointManagement;
		this.identityResolver = identityResolver;
		this.engineHelper = engineHelper;
	}



	@Override
	public Collection<AuthenticatorTypeDescription> getAuthenticatorTypes(String bindingId)
			throws EngineException
	{
		if (bindingId == null)
			return authReg.getAuthenticators();
		return authReg.getAuthenticatorsByBinding(bindingId);
	}

	@Override
	public AuthenticatorInstance createAuthenticator(String id, String typeId, String jsonVerificatorConfig,
			String jsonRetrievalConfig) throws EngineException
	{
		AuthenticatorImpl authenticator = new AuthenticatorImpl(identityResolver, authReg, id, typeId, 
				jsonRetrievalConfig, jsonVerificatorConfig);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			byte[] contents = authenticator.getSerializedConfiguration().getBytes(Constants.UTF); 
			dbGeneric.addObject(id, AUTHENTICATOR_OBJECT_TYPE, typeId, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return authenticator.getAuthenticatorInstance();
	}

	@Override
	public Collection<AuthenticatorInstance> getAuthenticators(String bindingId)
			throws EngineException
	{
		List<AuthenticatorInstance> ret = new ArrayList<AuthenticatorInstance>();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(AUTHENTICATOR_OBJECT_TYPE, sql);
			for (GenericObjectBean rawA: raw)
			{
				AuthenticatorImpl authenticator = engineHelper.getAuthenticatorNoCheck(rawA, sql); 
				ret.add(authenticator.getAuthenticatorInstance());
			}
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return ret;
	}

	@Override
	public void updateAuthenticator(String id, String jsonVerificatorConfig,
			String jsonRetrievalConfig) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AuthenticatorImpl current = engineHelper.getAuthenticator(id, sql);
			current.setConfiguration(jsonRetrievalConfig, jsonVerificatorConfig);
			byte []contents = current.getSerializedConfiguration().getBytes(Constants.UTF);
			dbGeneric.updateObject(id, AUTHENTICATOR_OBJECT_TYPE, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeAuthenticator(String id) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> fromDb = dbGeneric.getObjectsOfType(
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			for (GenericObjectBean raw: fromDb)
			{
				EndpointInstance instance = internalEndpointManagement.recreateEndpointFromDB(
						raw.getId()+"", raw.getName(), raw.getContents());
				List<AuthenticatorSet> used = instance.getEndpointDescription().getAuthenticatorSets();
				for (AuthenticatorSet set: used)
					if (set.getAuthenticators().contains(id))
						throw new IllegalArgumentException("The authenticator " + id + 
								" is used by the endpoint " + 
								instance.getEndpointDescription().getId());
			}

			dbGeneric.removeObject(id, AUTHENTICATOR_OBJECT_TYPE, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	
	@Override
	public Collection<CredentialType> getCredentialTypes() throws EngineException
	{
		return authReg.getLocalCredentialTypes();
	}

	@Override
	public CredentialRequirements addCredentialRequirement(String name, String description,
			Set<CredentialDefinition> configuredCredentials)
			throws EngineException
	{
		CredentialRequirementsHolder helper = new CredentialRequirementsHolder(name, description, 
				configuredCredentials, authReg);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			setCredentialRequirements(helper, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return helper.getCredentialRequirements();
	}

	@Override
	public Collection<CredentialRequirements> getCredentialRequirements()
			throws EngineException
	{
		List<CredentialRequirements> ret = new ArrayList<CredentialRequirements>();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(CREDENTIAL_REQ_OBJECT_TYPE, sql);
			for (GenericObjectBean rawA: raw)
			{
				CredentialRequirementsHolder helper = engineHelper.resolveCredentialRequirementsBean(rawA, sql);
				ret.add(helper.getCredentialRequirements());
			}
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return ret;
	}
	
	@Override
	public void updateCredentialRequirement(CredentialRequirements updated,
			LocalAuthenticationState desiredAuthnState) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(updated.getName(), 
					AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sql);
			if (raw == null)
				throw new RuntimeEngineException("There is no credential requirement with the name " + updated.getName());

			Set<Long> entities = engineHelper.getEntitiesByAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
					updated.getName(), sql);
			CredentialRequirementsHolder newCredReqs = new CredentialRequirementsHolder(updated.getName(),
					updated.getDescription(), updated.getRequiredCredentials(), authReg);
			for (Long entityId: entities)
			{
				engineHelper.updateEntityCredentialState(entityId, desiredAuthnState, newCredReqs, sql);
			}
			setCredentialRequirements(newCredReqs, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeCredentialRequirement(String toRemove, String replacementId,
			LocalAuthenticationState desiredAuthnState)
			throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(toRemove, 
					AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sql);
			if (raw == null)
				throw new RuntimeEngineException("There is no credential requirement with the name " + toRemove);

			Set<Long> entities = engineHelper.getEntitiesByAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
					toRemove, sql);
			if (entities.size() > 0 && replacementId == null)
				throw new IllegalCredentialException("There are entities with the removed credential requirements set and a replacement was not specified.");
			if (replacementId != null)
			{
				CredentialRequirementsHolder newCredReqs = engineHelper.getCredentialRequirements(replacementId, sql);
				
				for (Long entityId: entities)
				{
					engineHelper.updateEntityCredentialState(entityId, desiredAuthnState, newCredReqs, sql);
					engineHelper.setEntityCredentialRequirementsNoCheck(entityId, replacementId, sql);
				}
			}
			
			dbGeneric.removeObject(toRemove, AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sql);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	
	
	private void setCredentialRequirements(CredentialRequirementsHolder newCredReqs, SqlSession sql)
	{
		byte[] contents = newCredReqs.getSerializedConfiguration().getBytes(Constants.UTF); 
		dbGeneric.addObject(newCredReqs.getCredentialRequirements().getName(), 
				CREDENTIAL_REQ_OBJECT_TYPE, null, contents, sql);
	}
}
