/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Authentication flow management implementation.
 * @author P.Piernik
 *
 */

@Component
@Primary
@InvocationEventProducer
@Transactional
public class AuthenticationFlowManagementImpl implements AuthenticationFlowManagement
{

	private AuthenticationFlowDB authnFlowDB;
	private AuthorizationManager authz;
	private AuthenticatorInstanceDB authenticatorDB;
	private AttributeDAO dbAttributes;
	
	@Autowired
	public AuthenticationFlowManagementImpl(AuthenticationFlowDB authnFlowDB,
			AuthorizationManager authz, AuthenticatorInstanceDB authenticatorDB, AttributeDAO dbAttributes)
	{
		
		this.authnFlowDB = authnFlowDB;
		this.authz = authz;
		this.authenticatorDB = authenticatorDB;
		this.dbAttributes = dbAttributes;
	}

	
	@Override
	public void addAuthenticationFlowDefinition(
			AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);	
		assertIfAuthenticatorsExists(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		assertAuthenticatorsHaveTheSameBinding(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		authnFlowDB.create(authFlowdef);	
	}

	@Override
	public void removeAuthenticationFlowDefinition(String toRemove) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		authnFlowDB.delete(toRemove);
		
	}

	@Override
	public Collection<AuthenticationFlowDefinition> getAuthenticationFlows() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return authnFlowDB.getAll();
	}


	@Override
	public void updateAuthenticationFlowDefinition(AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIfAuthenticatorsExists(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		assertAuthenticatorsHaveTheSameBinding(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		authnFlowDB.update(authFlowdef);	
	}
	
	private void assertAuthenticatorsHaveTheSameBinding(Collection<String> toCheck, String flowName)
	{
		Map<String, AuthenticatorInstance> all = authenticatorDB.getAllAsMap();
		HashSet<String> bindings = new HashSet<>();
		
		
		for (String authName : toCheck)
		{
			bindings.add(all.get(authName).getTypeDescription().getSupportedBinding());
		}
	
		if (bindings.size() > 1)
		{	throw new IllegalArgumentException(
					"Can not add authentication flow " + flowName
							+ ", authenticators have different bindings");
		}
	
	}
	
	private void assertIfAuthenticatorsExists(Collection<String> toCheck, String flowName)
			throws EngineException
	{
		List<AuthenticatorInstance> all = authenticatorDB.getAll();
		List<String> allIds = all.stream().map(a -> a.getId()).collect(Collectors.toList());
		for (String toCheckId : toCheck)
		{
			if (!allIds.contains(toCheckId))
			{
				throw new IllegalArgumentException(
						"Can not add authentication flow " + flowName
								+ ", authenticator " + toCheckId
								+ " is undefined");
			}
		}

	}

	@Override
	public boolean getUserMFAOptIn(long entityId) throws EngineException
	{
		Optional<Boolean> userOptin = getUserOptinInternal(entityId);
		if (userOptin.isPresent())
		{
			return userOptin.get();
		}
		return false;
	}

	private Optional<Boolean> getUserOptinInternal(long entityId) throws EngineException
	{
		Collection<StoredAttribute> userOptin;

		userOptin = dbAttributes.getAttributes(
				UserMFAOptInAttributeTypeProvider.USER_MFA_OPT_IN, entityId, "/");

		if (userOptin.size() == 0)
			return Optional.empty();

		StoredAttribute attr = userOptin.iterator().next();
		List<?> values = attr.getAttribute().getValues();
		return values.size() > 0 ? Optional.of(Boolean.valueOf((String) values.get(0)))
				: Optional.of(false);
	}

	@Override
	public void setUserMFAOptIn(long entityId, boolean value) throws EngineException
	{

		if (value == false)
		{
			if (getUserOptinInternal(entityId).isPresent())
			{
				dbAttributes.deleteAttribute(
						UserMFAOptInAttributeTypeProvider.USER_MFA_OPT_IN,
						entityId, "/");
			}
		} else
		{
			Attribute sa = StringAttribute.of(
					UserMFAOptInAttributeTypeProvider.USER_MFA_OPT_IN, "/",
					String.valueOf(value));
			AttributeExt atExt = new AttributeExt(sa, true, new Date(), new Date());
			if (getUserOptinInternal(entityId).isPresent())
				dbAttributes.updateAttribute(new StoredAttribute(atExt, entityId));
			else
				dbAttributes.create(new StoredAttribute(atExt, entityId));
		}
	}
}
