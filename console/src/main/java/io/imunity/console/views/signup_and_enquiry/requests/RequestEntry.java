/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.requests;

import java.util.function.Function;

import io.imunity.vaadin.elements.grid.FilterableEntry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.engine.api.registration.RequestType;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;

public class RequestEntry implements FilterableEntry
{
	public final UserRequestState<?> request;
	private final MessageSource msg;
	private final String identity;

	public RequestEntry(UserRequestState<?> request, MessageSource msg, String identity)
	{
		this.request = request;
		this.msg = msg;
		this.identity = identity;
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

	@Override
	public boolean anyFieldContains(String searched, Function<String, String>  msg)
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