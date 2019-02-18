/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Authentication flow management implementation.
 * @author P.Piernik
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class AuthenticationFlowManagementImpl implements AuthenticationFlowManagement
{

	private AuthenticationFlowDB authnFlowDB;
	private InternalAuthorizationManager authz;
	private AuthenticatorConfigurationDB authenticatorDB;
	private AttributeDAO dbAttributes;
	
	@Autowired
	public AuthenticationFlowManagementImpl(AuthenticationFlowDB authnFlowDB,
			InternalAuthorizationManager authz, AuthenticatorConfigurationDB authenticatorDB, AttributeDAO dbAttributes)
	{
		
		this.authnFlowDB = authnFlowDB;
		this.authz = authz;
		this.authenticatorDB = authenticatorDB;
		this.dbAttributes = dbAttributes;
	}

	
	@Override
	public void addAuthenticationFlow(
			AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);	
		
		if (authenticatorDB.getAllAsMap().get(authFlowdef.getName()) != null)
		{
			throw new IllegalArgumentException(
					"Can not add authentication flow " + authFlowdef.getName()
							+ ", authenticator with the same name exists");
		}
		
		assertIfAuthenticatorsExists(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		authFlowdef.setRevision(0);
		authnFlowDB.create(authFlowdef);	
	}

	@Override
	public void removeAuthenticationFlow(String toRemove) throws EngineException
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
	public AuthenticationFlowDefinition getAuthenticationFlow(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return authnFlowDB.get(name);
	}

	@Override
	public void updateAuthenticationFlow(AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIfAuthenticatorsExists(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		
		AuthenticationFlowDefinition current = authnFlowDB.get(authFlowdef.getName());
		authFlowdef.setRevision(current.getRevision() + 1);	
		authnFlowDB.update(authFlowdef);	
	}
	
	private void assertIfAuthenticatorsExists(Set<String> toCheck, String flowName)
			throws EngineException
	{
		Set<String> existing = authenticatorDB.getAllNames();
		SetView<String> difference = Sets.difference(toCheck, existing);
		if (!difference.isEmpty())
			throw new IllegalArgumentException(
					"Can not add authentication flow " + flowName
					+ ", containing undefined authenticator(s) " + difference);
	}

	@Override
	public boolean getUserMFAOptIn(long entityId) throws EngineException
	{
		Optional<Boolean> userOptin = getUserOptinInternal(entityId);
		return userOptin.orElse(false);
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
