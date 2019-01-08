/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.engine.api.translation.in.EntityChange;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

/**
 * Output of {@link RegistrationRequest} or {@link EnquiryResponse} translation with a 
 * corresponding translation profile.
 * <p>
 * Contains data that will be used to create a new entity when the request is accepted or data to update 
 * an existing entity (in case of enquires).
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
	
	private AutomaticRequestAction autoAction = AutomaticRequestAction.none;
	
	private Map<String, IdentityParam> identities = new HashMap<>();
	private Map<String, Attribute> attributes = new HashMap<>();
	private Map<String, GroupParam> groups = new HashMap<>();
	private Map<String, Set<String>> attributeClasses = new HashMap<>();
	private EntityChange entityChange;
	private EntityState entityState = EntityState.valid;
	private String credentialRequirement;
	private String redirectURL = null;
	private I18nMessage postSubmitMessage;
	private AutomaticInvitationProcessingParam invitationProcessing;
	
	public TranslatedRegistrationRequest(String credentialRequirement)
	{
		this.credentialRequirement = credentialRequirement;
	}
	
	public TranslatedRegistrationRequest()
	{
	}
	
	public void addIdentity(IdentityParam identity)
	{
		identities.put(identity.getTypeId()+"_"+identity.getValue(), identity);
	}
	public void removeIdentity(IdentityParam identity)
	{
		identities.remove(identity.getTypeId()+"_"+identity.getValue());
	}
	public void addAttribute(Attribute attribute)
	{
		attributes.put(attribute.getGroupPath()+"//"+attribute.getName(), attribute);
	}
	public void removeAttribute(Attribute attribute)
	{
		attributes.remove(attribute.getGroupPath()+"//"+attribute.getName());
	}
	public void addMembership(GroupParam group)
	{
		groups.put(group.getGroup(), group);
	}
	public void removeMembership(String group)
	{
		groups.remove(group);
	}
	public void addAttributeClass(String group, String ac)
	{
		Set<String> acs = attributeClasses.get(group);
		if (acs == null)
		{
			acs = new HashSet<>();
			attributeClasses.put(group, acs);
		}
		acs.add(ac);
	}
	
	public void setAutoAction(AutomaticRequestAction autoAction)
	{
		this.autoAction = autoAction;
	}

	public void setEntityChange(EntityChange entityChange)
	{
		this.entityChange = entityChange;
	}

	public void setEntityState(EntityState entityState)
	{
		this.entityState = entityState;
	}

	public AutomaticRequestAction getAutoAction()
	{
		return autoAction;
	}

	public Collection<IdentityParam> getIdentities()
	{
		return identities.values();
	}

	public Collection<Attribute> getAttributes()
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

	public EntityState getEntityState()
	{
		return entityState;
	}

	public String getCredentialRequirement()
	{
		return credentialRequirement;
	}

	public String getRedirectURL()
	{
		return redirectURL;
	}

	public void setRedirectURL(String redirectURL)
	{
		this.redirectURL = redirectURL;
	}

	public void setCredentialRequirement(String credentialRequirement)
	{
		this.credentialRequirement = credentialRequirement;
	}

	public Map<String, Set<String>> getAttributeClasses()
	{
		return attributeClasses;
	}

	public I18nMessage getPostSubmitMessage()
	{
		return postSubmitMessage;
	}

	public void setPostSubmitMessage(I18nMessage message)
	{
		this.postSubmitMessage = message;
	}

	public AutomaticInvitationProcessingParam getInvitationProcessing()
	{
		return invitationProcessing;
	}

	public void setInvitationProcessing(AutomaticInvitationProcessingParam invitationProcessing)
	{
		this.invitationProcessing = invitationProcessing;
	}
}
