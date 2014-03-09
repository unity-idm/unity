/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceDB;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.engine.authn.AuthenticatorImpl;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.engine.authn.CredentialHolder;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.endpoints.EndpointsUpdater;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
public class AuthenticationManagementImpl implements AuthenticationManagement
{
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private DBSessionManager db;
	private AuthenticatorInstanceDB authenticatorDB;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialRequirementDB;
	private IdentityResolver identityResolver;
	private EngineHelper engineHelper;
	private EndpointsUpdater endpointsUpdater;
	private AuthenticatorLoader authenticatorLoader;
	private DBAttributes dbAttributes;
	private AuthorizationManager authz;
	
	@Autowired
	public AuthenticationManagementImpl(AuthenticatorsRegistry authReg, DBSessionManager db,
			AuthenticatorInstanceDB authenticatorDB,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB,
			IdentityResolver identityResolver, EngineHelper engineHelper,
			EndpointsUpdater endpointsUpdater, AuthenticatorLoader authenticatorLoader,
			DBAttributes dbAttributes, AuthorizationManager authz, LocalCredentialsRegistry localCredReg)
	{
		this.authReg = authReg;
		this.localCredReg = localCredReg;
		this.db = db;
		this.authenticatorDB = authenticatorDB;
		this.credentialDB = credentialDB;
		this.credentialRequirementDB = credentialRequirementDB;
		this.identityResolver = identityResolver;
		this.engineHelper = engineHelper;
		this.endpointsUpdater = endpointsUpdater;
		this.authenticatorLoader = authenticatorLoader;
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
		SqlSession sql = db.getSqlSession(true);
		AuthenticatorImpl authenticator;
		try
		{
			if (credentialName != null)
			{
				CredentialDefinition credentialDef = credentialDB.get(credentialName, sql);
				CredentialHolder credential = new CredentialHolder(credentialDef, localCredReg);
				String credentialConfiguration = credential.getCredentialDefinition().getJsonConfiguration();
				authenticator = new AuthenticatorImpl(identityResolver, authReg, id, typeId, 
						jsonRetrievalConfig, credentialName, credentialConfiguration);
				
				verifyIfLocalCredentialMatchesVerificator(authenticator, credential, 
						credentialName);
			} else
			{
				authenticator = new AuthenticatorImpl(identityResolver, authReg, id, typeId, 
						jsonRetrievalConfig, jsonVerificatorConfig);
			}
			authenticatorDB.insert(authenticator.getAuthenticatorInstance().getId(), 
					authenticator.getAuthenticatorInstance(), sql);
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
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<AuthenticatorInstance> ret = authenticatorDB.getAll(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateAuthenticator(String id, String jsonVerificatorConfig,
			String jsonRetrievalConfig, String localCredential) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AuthenticatorImpl current = authenticatorLoader.getAuthenticator(id, sql);
			if (localCredential != null)
			{
				CredentialDefinition credentialDef = credentialDB.get(localCredential, sql);
				CredentialHolder credential = new CredentialHolder(credentialDef, localCredReg);
				jsonVerificatorConfig = credential.getCredentialDefinition().getJsonConfiguration();
				verifyIfLocalCredentialMatchesVerificator(current, credential, 
						localCredential);
			}
			
			current.updateConfiguration(jsonRetrievalConfig, jsonVerificatorConfig, localCredential);
			authenticatorDB.update(id, current.getAuthenticatorInstance(), sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		endpointsUpdater.updateEndpoints();
	}

	private void verifyIfLocalCredentialMatchesVerificator(AuthenticatorImpl authenticator,
			CredentialHolder credential, String requestedLocalCredential) throws IllegalCredentialException
	{
		String verificationMethod = authenticator.getAuthenticatorInstance().
				getTypeDescription().getVerificationMethod();
		if (!credential.getCredentialDefinition().getTypeId().equals(verificationMethod))
			throw new IllegalCredentialException("The local credential " + requestedLocalCredential + 
					"is of different type then the credential suported by the " +
					"authenticator, which is " + verificationMethod);
	}
	
	@Override
	public void removeAuthenticator(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			authenticatorDB.remove(id, sql);
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
		return localCredReg.getLocalCredentialTypes();
	}

	@Override
	public void addCredentialRequirement(CredentialRequirements toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Set<String> existingCreds = credentialDB.getAllNames(sql);
			for (String u: toAdd.getRequiredCredentials())
				if (!existingCreds.contains(u))
					throw new IllegalCredentialException("The credential " + u + " is unknown");
			credentialRequirementDB.insert(toAdd.getName(), toAdd, sql);
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
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<CredentialRequirements> ret = credentialRequirementDB.getAll(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	@Override
	public void updateCredentialRequirement(CredentialRequirements updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		SqlSession sql = db.getSqlSession(true);
		try
		{
			Map<String, CredentialDefinition> credDefs = credentialDB.getAllAsMap(sql);
			CredentialRequirementsHolder.checkCredentials(updated, credDefs, localCredReg);
			credentialRequirementDB.update(updated.getName(), updated, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeCredentialRequirement(String toRemove, String replacementId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		SqlSession sql = db.getSqlSession(true);
		try
		{
			Set<Long> entities = engineHelper.getEntitiesByAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
					Collections.singleton(toRemove), sql);
			if (entities.size() > 0 && replacementId == null)
				throw new IllegalCredentialException("There are entities with the removed credential requirements set and a replacement was not specified.");
			if (replacementId != null)
			{
				for (Long entityId: entities)
					engineHelper.setEntityCredentialRequirementsNoCheck(entityId, replacementId, sql);
			}

			credentialRequirementDB.remove(toRemove, sql);
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
		CredentialHolder helper = new CredentialHolder(credentialDefinition, localCredReg);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			credentialDB.insert(credentialDefinition.getName(), credentialDefinition, sql);
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
			LocalCredentialState desiredAuthnState) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		CredentialHolder helper = new CredentialHolder(updated, localCredReg);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			//get all cred reqs with it
			Set<String> affectedCr = new HashSet<String>();
			List<CredentialRequirements> crs = credentialRequirementDB.getAll(sql);
			for (CredentialRequirements cr: crs)
			{
				if (cr.getRequiredCredentials().contains(updated.getName()))
					affectedCr.add(cr.getName());
			}
			
			//get all entities with any of the affected CRs
			Set<Long> entities = engineHelper.getEntitiesByAttribute(
					SystemAttributeTypes.CREDENTIAL_REQUIREMENTS, affectedCr, sql);
			
			//check if the entities credential status will be fine
			for (Long entityId: entities)
			{
				engineHelper.checkEntityCredentialState(entityId, desiredAuthnState, helper, sql);
			}
			
			credentialDB.update(updated.getName(), updated, sql);
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
			credentialDB.remove(toRemove, sql);
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
			ret = credentialDB.getAll(sql);
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
