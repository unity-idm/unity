/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.requests;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.registration.RequestType;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/**
 * Represents grid request entry
 * 
 * @author P.Piernik
 *
 */
public class RequestEntry implements FilterableEntry
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, RequestEntry.class);
	
	public UserRequestState<?> request;
	private MessageSource msg;
	private String identity;

	public RequestEntry(UserRequestState<?> request, MessageSource msg, EntityManagement idMan)
	{
		this.request = request;
		this.msg = msg;
		this.identity = resolveIdentity(idMan);
	}

	public String getTypeAsString()
	{
		boolean enquiry = request instanceof EnquiryResponseState;
		return msg.getMessage("RegistrationRequest.type." + (enquiry ? "enquiry" : "registration"));
	}

	public RequestType getType()
	{
		return request instanceof EnquiryResponseState ? RequestType.Enquiry : RequestType.Registration;
	}

	public String getForm()
	{
		return request.getRequest().getFormId();
	}

	public String getRequestId()
	{
		return request.getRequestId();
	}

	public String getSubmitTime()
	{
		return TimeUtil.formatStandardInstant(request.getTimestamp().toInstant());
	}

	public String getStatus()
	{
		return msg.getMessage("RegistrationRequestStatus." + request.getStatus());
	}

	public String getIdentity()
	{
		return identity;
	}

	private String resolveIdentity(EntityManagement idMan)
	{
		if (getType().equals(RequestType.Registration))
		{
			List<IdentityParam> identities = request.getRequest().getIdentities();
			if (identities.isEmpty())
				return "-";
			IdentityParam id = identities.get(0);
			return id == null ? "-" : id.toHumanReadableString();
		} else
		{
			EnquiryResponseState enqRequest = (EnquiryResponseState) request;
			Entity entity;
			try
			{
				entity = idMan.getEntity(new EntityParam(enqRequest.getEntityId()));
				List<Identity> identities = entity.getIdentities();
				VerifiableElementBase email = getEmailIdentity(identities.stream()
						.map(i -> (IdentityParam) i).collect(Collectors.toList()));
				if (email != null)
				{
					return email.getValue();
				} else
				{
					return identities.stream().findFirst().get().toHumanReadableString();
				}

			} catch (Exception e)
			{
				LOG.error("Failed to resolve identity {}", identity, e);
				return "-";
			}
		}
	}
	
	private VerifiableElementBase getEmailIdentity(List<IdentityParam> identities)
	{
		for (IdentityParam id : identities)
		{
			if (id != null && id.getTypeId().equals(EmailIdentity.ID))
				return new VerifiableElementBase(id.getValue(), id.getConfirmationInfo());
		}
		return null;
	}

	@Override
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (getTypeAsString() != null && getTypeAsString().toLowerCase().contains(textLower))
			return true;

		if (getForm() != null && getForm().toLowerCase().contains(textLower))
			return true;

		if (getIdentity() != null && getIdentity().toLowerCase().contains(textLower))
			return true;

		if (getSubmitTime() != null && getSubmitTime().toLowerCase().contains(textLower))
			return true;

		if (getStatus() != null && getStatus().toLowerCase().contains(textLower))
			return true;

		return false;
	}
}