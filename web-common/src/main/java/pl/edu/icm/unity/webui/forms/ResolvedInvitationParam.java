/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationParam;
import pl.edu.icm.unity.base.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;

public class ResolvedInvitationParam
{
	public static final List<String> NOT_ANONYMOUS_IDENTITIES_TYPES = Collections
			.unmodifiableList(Arrays.asList(EmailIdentity.ID, UsernameIdentity.ID, X500Identity.ID));

	public final String code;
	public final Set<Entity> entities;
	public final String contactAddress;
	private final InvitationParam invitationParam;

	ResolvedInvitationParam(Set<Entity> entities, String code, InvitationParam invitationParam)
	{
		this.entities =  Collections.unmodifiableSet(entities);
		this.invitationParam = invitationParam;
		this.contactAddress = invitationParam.getContactAddress();
		this.code = code;
	}

	public RegistrationInvitationParam getAsRegistration()
	{
		if (invitationParam.getType().equals(InvitationType.REGISTRATION))
			return (RegistrationInvitationParam) invitationParam;

		if (invitationParam.getType().equals(InvitationType.COMBO))
		{
			return ((ComboInvitationParam) invitationParam).getAsRegistration();
		}

		throw new UnsupportedOperationException("Enquiry invitation only");
	}

	public EnquiryInvitationParam getAsEnquiryInvitationParamWithAnonymousEntity()
	{
		return getAsEnquiryInvitationParam(null);
	}
	
	public EnquiryInvitationParam getAsEnquiryInvitationParam(Long entity)
	{
		if (invitationParam.getType().equals(InvitationType.ENQUIRY))
			return (EnquiryInvitationParam) invitationParam;

		if (invitationParam.getType().equals(InvitationType.COMBO))
		{
			return ((ComboInvitationParam) invitationParam).getAsEnquiry(entity);
		}

		throw new UnsupportedOperationException("Registration invitation only");
	}

	public boolean canBeProcessedAsEnquiryWithResolvedUser()
	{
		return invitationParam.getType().equals(InvitationType.COMBO) && !entities.isEmpty();
	}
	
	public InvitationType getType()
	{
		return invitationParam.getType();
	}
	
	public void assertMatchToForm(BaseForm form) throws RegCodeException
	{
		try
		{
			if (!invitationParam.matchesForm(form))
				throw new RegCodeException(ErrorCause.INVITATION_OF_OTHER_FORM);
		} catch (IllegalFormTypeException e)
		{
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		}
	}
	
	public List<Entity> getEntitiesWithoutAnonymous()
	{
		return entities.stream().filter(e -> e.getIdentities().stream()
				.filter(i -> !i.isLocal() || i.isLocal() && NOT_ANONYMOUS_IDENTITIES_TYPES.contains(i.getTypeId()))
				.count() > 0).collect(Collectors.toList());
	}
}
