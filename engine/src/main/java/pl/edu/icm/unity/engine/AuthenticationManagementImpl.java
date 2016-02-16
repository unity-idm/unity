/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
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
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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
@InvocationEventProducer
public class AuthenticationManagementImpl implements AuthenticationManagement
{
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private AuthenticatorInstanceDB authenticatorDB;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialRequirementDB;
	private IdentityResolver identityResolver;
	private EngineHelper engineHelper;
	private EndpointsUpdater endpointsUpdater;
	private AuthenticatorLoader authenticatorLoader;
	private DBAttributes dbAttributes;
	private AuthorizationManager authz;
	private UnityMessageSource msg;
	private TransactionalRunner tx;
	
	@Autowired
	public AuthenticationManagementImpl(AuthenticatorsRegistry authReg, TransactionalRunner tx,
			AuthenticatorInstanceDB authenticatorDB,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB,
			IdentityResolver identityResolver, EngineHelper engineHelper,
			EndpointsUpdater endpointsUpdater, AuthenticatorLoader authenticatorLoader,
			DBAttributes dbAttributes, AuthorizationManager authz, LocalCredentialsRegistry localCredReg,
			UnityMessageSource msg)
	{
		this.authReg = authReg;
		this.tx = tx;
		this.localCredReg = localCredReg;
		this.authenticatorDB = authenticatorDB;
		this.credentialDB = credentialDB;
		this.credentialRequirementDB = credentialRequirementDB;
		this.identityResolver = identityResolver;
		this.engineHelper = engineHelper;
		this.endpointsUpdater = endpointsUpdater;
		this.authenticatorLoader = authenticatorLoader;
		this.dbAttributes = dbAttributes;
		this.authz = authz;
		this.msg = msg;
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
	@Transactional
	public AuthenticatorInstance createAuthenticator(String id, String typeId, String jsonVerificatorConfig,
			String jsonRetrievalConfig, String credentialName) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		AuthenticatorImpl authenticator;
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
		return authenticator.getAuthenticatorInstance();
	}

	@Override
	@Transactional(autoCommit=false)
	public Collection<AuthenticatorInstance> getAuthenticators(String bindingId)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		List<AuthenticatorInstance> ret = authenticatorDB.getAll(sql);
		sql.commit();
		if (bindingId != null)
		{
			for (Iterator<AuthenticatorInstance> iter = ret.iterator(); iter.hasNext();)
			{
				AuthenticatorInstance authnInstance = iter.next();
				if (!bindingId.equals(authnInstance.getTypeDescription().getSupportedBinding()))
				{
					iter.remove();
				}
			}
		}
		return ret;
	}

	@Override
	public void updateAuthenticator(String id, String jsonVerificatorConfig,
			String jsonRetrievalConfig, String localCredential) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		tx.runInTransaction(() -> {
			SqlSession sql = SqlSessionTL.get();
			AuthenticatorImpl current = authenticatorLoader.getAuthenticator(id, sql);
			String verificatorConfig = jsonVerificatorConfig;
			if (localCredential != null)
			{
				CredentialDefinition credentialDef = credentialDB.get(localCredential, sql);
				CredentialHolder credential = new CredentialHolder(credentialDef, localCredReg);
				verificatorConfig = credential.getCredentialDefinition().getJsonConfiguration();
				verifyIfLocalCredentialMatchesVerificator(current, credential, 
						localCredential);
			}
			
			current.updateConfiguration(jsonRetrievalConfig, verificatorConfig, localCredential);
			authenticatorDB.update(id, current.getAuthenticatorInstance(), sql);
		});
		endpointsUpdater.updateManual();
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
	@Transactional
	public void removeAuthenticator(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		authenticatorDB.remove(id, sql);
	}

	
	@Override
	public Collection<CredentialType> getCredentialTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return localCredReg.getLocalCredentialTypes();
	}

	@Override
	@Transactional
	public void addCredentialRequirement(CredentialRequirements toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		Set<String> existingCreds = credentialDB.getAllNames(sql);
		for (String u: toAdd.getRequiredCredentials())
			if (!existingCreds.contains(u))
				throw new IllegalCredentialException("The credential " + u + " is unknown");
		credentialRequirementDB.insert(toAdd.getName(), toAdd, sql);
	}

	
	@Override
	@Transactional
	public Collection<CredentialRequirements> getCredentialRequirements()
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = SqlSessionTL.get();
		return credentialRequirementDB.getAll(sql);
	}
	
	@Override
	@Transactional
	public void updateCredentialRequirement(CredentialRequirements updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		Map<String, CredentialDefinition> credDefs = credentialDB.getAllAsMap(sql);
		CredentialRequirementsHolder.checkCredentials(updated, credDefs, localCredReg);
		credentialRequirementDB.update(updated.getName(), updated, sql);
	}

	@Override
	@Transactional
	public void removeCredentialRequirement(String toRemove, String replacementId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();

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
	}


	@Override
	@Transactional
	public void addCredentialDefinition(CredentialDefinition credentialDefinition)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		CredentialHolder helper = new CredentialHolder(credentialDefinition, localCredReg);
		SqlSession sql = SqlSessionTL.get();
		credentialDB.insert(credentialDefinition.getName(), credentialDefinition, sql);
		AttributeType at = getCredentialAT(helper.getCredentialDefinition().getName());
		dbAttributes.addAttributeType(at, sql);
	}

	@Override
	@Transactional
	public void updateCredentialDefinition(CredentialDefinition updated,
			LocalCredentialState desiredAuthnState) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		CredentialHolder helper = new CredentialHolder(updated, localCredReg);
		SqlSession sql = SqlSessionTL.get();
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
	}


	@Override
	@Transactional
	public void removeCredentialDefinition(String toRemove) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		credentialDB.remove(toRemove, sql);
		dbAttributes.removeAttributeType(SystemAttributeTypes.CREDENTIAL_PREFIX+toRemove, true, sql);
	}


	@Override
	@Transactional
	public Collection<CredentialDefinition> getCredentialDefinitions() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = SqlSessionTL.get();
		return credentialDB.getAll(sql);
	}
	

	private AttributeType getCredentialAT(String name)
	{
		AttributeType credentialAt = new AttributeType(SystemAttributeTypes.CREDENTIAL_PREFIX+name, 
				new StringAttributeSyntax(), msg, SystemAttributeTypes.CREDENTIAL_PREFIX,
				new Object[] {name});
		credentialAt.setMaxElements(1);
		credentialAt.setMinElements(1);
		credentialAt.setVisibility(AttributeVisibility.local);
		credentialAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		return credentialAt;
	}
}
