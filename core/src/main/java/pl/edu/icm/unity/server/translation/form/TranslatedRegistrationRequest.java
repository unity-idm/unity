/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.server.translation.in.EntityChange;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

/**
 * Output of {@link RegistrationRequest} translation with a {@link RegistrationTranslationProfile}.
 * Contains data that will be used to create a new entity when the request is accepted.
 * Additionally contains information whether the request should be automatically processed.
 * <p>
 * Implementation ensures that the same attribute/group/identity is not added twice. 
 * It is checked taking into account only the relevant information, not the source (as remote idp).
 * 
 * @author K. Benedyczak
 */
public class TranslatedRegistrationRequest
{
	public enum AutomaticRequestAction {none, drop, reject, accept}
	
	private AutomaticRequestAction autoAction;
	
	private Map<String, IdentityParam> identities = new HashMap<>();
	private Map<String, Attribute<?>> attributes = new HashMap<>();
	private Map<String, GroupParam> groups = new HashMap<>();
	private EntityChange entityChange;
	private EntityState initialEntityState = EntityState.valid;
	private String credentialRequirement;
	
	public TranslatedRegistrationRequest(String credentialRequirement)
	{
		this.credentialRequirement = credentialRequirement;
	}
	
	public void addIdentity(IdentityParam identity)
	{
		identities.put(identity.getTypeId()+"_"+identity.getValue(), identity);
	}
	public void addAttribute(Attribute<?> attribute)
	{
		attributes.put(attribute.getGroupPath()+"//"+attribute.getName(), attribute);
	}
	public void addMembership(GroupParam group)
	{
		groups.put(group.getGroup(), group);
	}

	public void setAutoAction(AutomaticRequestAction autoAction)
	{
		this.autoAction = autoAction;
	}

	public void setEntityChange(EntityChange entityChange)
	{
		this.entityChange = entityChange;
	}

	public void setInitialEntityState(EntityState initialEntityState)
	{
		this.initialEntityState = initialEntityState;
	}

	public AutomaticRequestAction getAutoAction()
	{
		return autoAction;
	}

	public Collection<IdentityParam> getIdentities()
	{
		return identities.values();
	}

	public Collection<Attribute<?>> getAttributes()
	{
		return attributes.values();
	}

	public Collection<GroupParam> getGroups()
	{
		return groups.values();
	}

	public EntityChange getEntityChange()
	{
		return entityChange;
	}

	public EntityState getInitialEntityState()
	{
		return initialEntityState;
	}

	public String getCredentialRequirement()
	{
		return credentialRequirement;
	}
}
