/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authn.AuthenticatorImpl;
import pl.edu.icm.unity.engine.authn.CredentialHolder;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsSerializer;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.EndpointsUpdater;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.engine.internal.InternalEndpointManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
public class AuthenticationManagementImpl implements AuthenticationManagement
{
	public static final String AUTHENTICATOR_OBJECT_TYPE = "authenticator";
	public static final String CREDENTIAL_REQ_OBJECT_TYPE = "credentialRequirement";
	public static final String CREDENTIAL_OBJECT_TYPE = "credential";
	private AuthenticatorsRegistry authReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private InternalEndpointManagement internalEndpointManagement;
	private IdentityResolver identityResolver;
	private EngineHelper engineHelper;
	private EndpointsUpdater endpointsUpdater;
	private DBAttributes dbAttributes;
	private AuthorizationManager authz;
	
	@Autowired
	public AuthenticationManagementImpl(AuthenticatorsRegistry authReg, DBSessionManager db,
			DBGeneric dbGeneric, InternalEndpointManagement internalEndpointManagement,
			IdentityResolver identityResolver, EngineHelper engineHelper,
			EndpointsUpdater endpointsUpdater, DBAttributes dbAttributes,
			AuthorizationManager authz)
	{
		this.authReg = authReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.internalEndpointManagement = internalEndpointManagement;
		this.identityResolver = identityResolver;
		this.engineHelper = engineHelper;
		this.endpointsUpdater = endpointsUpdater;
		this.dbAttributes = dbAttributes;
		this.authz = authz;
	}



	@Override
	public Collection<AuthenticatorTypeDescription> getAuthenticatorTypes(String bindingId)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		if (bindingId == null)
			return authReg.getAuthenticators();
		return authReg.getAuthenticatorsByBinding(bindingId);
	}

	@Override
	public AuthenticatorInstance createAuthenticator(String id, String typeId, String jsonVerificatorConfig,
			String jsonRetrievalConfig, String credentialName) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		AuthenticatorImpl authenticator = new AuthenticatorImpl(identityResolver, authReg, id, typeId, 
				jsonRetrievalConfig, jsonVerificatorConfig);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			if (authenticator.getAuthenticatorInstance().getTypeDescription().isLocal())
			{
				GenericObjectBean raw = dbGeneric.getObjectByNameType(credentialName, CREDENTIAL_OBJECT_TYPE, sql);
				if (raw == null)
					throw new IllegalCredentialException("There is no credential defined " +
							"with the name " + credentialName );
				CredentialHolder credential = engineHelper.resolveCredentialBean(raw, sql);
				String verificationMethod = authenticator.getAuthenticatorInstance().
						getTypeDescription().getVerificationMethod();
				if (!credential.getCredentialDefinition().getTypeId().equals(verificationMethod))
					throw new IllegalCredentialException("The local credential " + credentialName + 
							"is of different type then the credential suported by the " +
							"authenticator, which is " + verificationMethod);
				authenticator.setCredentialName(credentialName);
			}
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
		authz.checkAuthorization(AuthzCapability.readInfo);
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
		authz.checkAuthorization(AuthzCapability.maintenance);
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
		endpointsUpdater.updateEndpoints();
	}

	@Override
	public void removeAuthenticator(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> fromDb = dbGeneric.getObjectsOfType(
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			for (GenericObjectBean raw: fromDb)
			{
				EndpointInstance instance = internalEndpointManagement.deserializeEndpoint(
						raw.getName(), raw.getSubType(), raw.getContents(), sql);
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
		authz.checkAuthorization(AuthzCapability.readInfo);
		return authReg.getLocalCredentialTypes();
	}

	@Override
	public void addCredentialRequirement(CredentialRequirements toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> existingCredsB = dbGeneric.getObjectsOfType(CREDENTIAL_OBJECT_TYPE, sql);
			Set<String> existingCreds = new HashSet<String>();
			for (GenericObjectBean bean: existingCredsB)
				existingCreds.add(bean.getName());
			for (String u: toAdd.getRequiredCredentials())
				if (!existingCreds.contains(u))
					throw new IllegalCredentialException("The credential " + u + " is unknown");
			
			byte[] contents = CredentialRequirementsSerializer.serialize(toAdd);
			dbGeneric.addObject(toAdd.getName(), CREDENTIAL_REQ_OBJECT_TYPE, null, contents, sql);

			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	
	@Override
	public Collection<CredentialRequirements> getCredentialRequirements()
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		List<CredentialRequirements> ret = new ArrayList<CredentialRequirements>();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(CREDENTIAL_REQ_OBJECT_TYPE, sql);
			for (GenericObjectBean rawA: raw)
			{
				CredentialRequirements cr = CredentialRequirementsSerializer.deserialize(rawA.getContents());
				ret.add(cr);
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
		authz.checkAuthorization(AuthzCapability.maintenance);

		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(updated.getName(), 
					AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sql);
			if (raw == null)
				throw new IllegalCredentialException("There is no credential requirement with the name " + 
						updated.getName());

			Set<Long> entities = engineHelper.getEntitiesByAttribute(
					SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
					Collections.singleton(updated.getName()), sql);
			List<CredentialDefinition> credDefs = engineHelper.getCredentialDefinitions(sql);
			CredentialRequirementsHolder newCredReqs = new CredentialRequirementsHolder(updated, 
					authReg, credDefs);
			for (Long entityId: entities)
			{
				engineHelper.updateEntityCredentialState(entityId, desiredAuthnState, newCredReqs, sql);
			}
			byte[] contents = CredentialRequirementsSerializer.serialize(updated); 
			dbGeneric.updateObject(newCredReqs.getCredentialRequirements().getName(), 
					CREDENTIAL_REQ_OBJECT_TYPE, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeCredentialRequirement(String toRemove, String replacementId,
			LocalAuthenticationState desiredAuthnState) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(toRemove, 
					AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sql);
			if (raw == null)
				throw new IllegalCredentialException("There is no credential requirement with the name " + toRemove);

			Set<Long> entities = engineHelper.getEntitiesByAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
					Collections.singleton(toRemove), sql);
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


	@Override
	public void addCredentialDefinition(CredentialDefinition credentialDefinition)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		CredentialHolder helper = new CredentialHolder(credentialDefinition, authReg);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			byte[] contents = helper.getSerializedConfiguration().getBytes(Constants.UTF); 
			dbGeneric.addObject(helper.getCredentialDefinition().getName(), 
					CREDENTIAL_OBJECT_TYPE, null, contents, sql);
			AttributeType at = getCredentialAT(helper.getCredentialDefinition().getName());
			dbAttributes.addAttributeType(at, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateCredentialDefinition(CredentialDefinition updated,
			LocalAuthenticationState desiredAuthnState) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		CredentialHolder helper = new CredentialHolder(updated, authReg);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(updated.getName(), 
					AuthenticationManagementImpl.CREDENTIAL_OBJECT_TYPE, sql);
			if (raw == null)
				throw new IllegalCredentialException("There is no credential with the name " + updated.getName());

			//get all cred reqs with it
			List<GenericObjectBean> rawCr = dbGeneric.getObjectsOfType(CREDENTIAL_REQ_OBJECT_TYPE, sql);
			Set<String> affectedCr = new HashSet<String>();
			for (GenericObjectBean rawA: rawCr)
			{
				CredentialRequirements cr = CredentialRequirementsSerializer.deserialize(rawA.getContents());
				if (cr.getRequiredCredentials().contains(updated.getName()))
					affectedCr.add(cr.getName());
			}
			
			//get all entities with any of the affected CRs
			Set<Long> entities = engineHelper.getEntitiesByAttribute(
					SystemAttributeTypes.CREDENTIAL_REQUIREMENTS, affectedCr, sql);
			
			//update the entities authentication status
			for (Long entityId: entities)
			{
				engineHelper.updateEntityCredentialState(entityId, desiredAuthnState, helper, sql);
			}
			
			byte[] contents = helper.getSerializedConfiguration().getBytes(Constants.UTF);
			dbGeneric.updateObject(updated.getName(), CREDENTIAL_OBJECT_TYPE, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}


	@Override
	public void removeCredentialDefinition(String toRemove) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(toRemove, 
					AuthenticationManagementImpl.CREDENTIAL_OBJECT_TYPE, sql);
			if (raw == null)
				throw new IllegalCredentialException("There is no credential with the name " + toRemove);

			List<GenericObjectBean> authsRaw = dbGeneric.getObjectsOfType(AUTHENTICATOR_OBJECT_TYPE, sql);
			for (GenericObjectBean rawA: authsRaw)
			{
				AuthenticatorImpl authenticator = engineHelper.getAuthenticatorNoCheck(rawA, sql);
				if (toRemove.equals(authenticator.getAuthenticatorInstance().getLocalCredentialName()))
					throw new IllegalCredentialException("The credential is used by an authenticator " 
							+ authenticator.getAuthenticatorInstance().getId());
			}

			List<GenericObjectBean> crRaw = dbGeneric.getObjectsOfType(CREDENTIAL_REQ_OBJECT_TYPE, sql);
			for (GenericObjectBean rawA: crRaw)
			{
				CredentialRequirements cr = CredentialRequirementsSerializer.deserialize(rawA.getContents());
				if (cr.getRequiredCredentials().contains(toRemove))
					throw new IllegalCredentialException("The credential is used by a credential requirement " 
							+ cr.getName());
			}

			dbGeneric.removeObject(toRemove, AuthenticationManagementImpl.CREDENTIAL_OBJECT_TYPE, sql);
			dbAttributes.removeAttributeType(SystemAttributeTypes.CREDENTIAL_PREFIX+toRemove, true, sql);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}


	@Override
	public Collection<CredentialDefinition> getCredentialDefinitions() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);

		List<CredentialDefinition> ret;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			ret = engineHelper.getCredentialDefinitions(sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return ret;
	}
	
	

	private AttributeType getCredentialAT(String name)
	{
		AttributeType credentialAt = new AttributeType(SystemAttributeTypes.CREDENTIAL_PREFIX+name, 
				new StringAttributeSyntax());
		credentialAt.setMaxElements(1);
		credentialAt.setMinElements(1);
		credentialAt.setVisibility(AttributeVisibility.local);
		credentialAt.setDescription("Credential of " + name);
		credentialAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		return credentialAt;
	}
}
