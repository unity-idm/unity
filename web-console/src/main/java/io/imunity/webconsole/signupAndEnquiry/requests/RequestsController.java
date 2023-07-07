/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.requests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.registration.RequestType;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponsesChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestsChangedEvent;

/**
 * Controller for all registration and enquiry request views.
 * 
 * @author P.Piernik
 *
 */
@Component
class RequestsController
{
	private EntityManagement idMan;
	private RegistrationsManagement regMan;
	private EnquiryManagement enqMan;
	private MessageSource msg;

	@Autowired
	RequestsController(EntityManagement idMan, RegistrationsManagement regMan, EnquiryManagement enqMan,
			MessageSource msg)
	{
		this.idMan = idMan;
		this.regMan = regMan;
		this.enqMan = enqMan;
		this.msg = msg;
	}

	Collection<RequestEntry> getRequests() throws ControllerException
	{
		List<RequestEntry> res = new ArrayList<>();

		try
		{
			regMan.getRegistrationRequests().stream()
					.forEach(r -> res.add(new RequestEntry(r, msg, idMan)));
			enqMan.getEnquiryResponses().stream().forEach(r -> res.add(new RequestEntry(r, msg, idMan)));

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("RequestsController.getAllError"), e);
		}

		return res;
	}

	public void process(Collection<?> items, RegistrationRequestAction action, EventsBus bus)
			throws ControllerException
	{
		
		Set<RequestType> types = new HashSet<>();
		for (Object item : items)
		{
			try
			{
				types.add(processSingle((RequestEntry) item, action, bus).type);
			} catch (EngineException e)
			{
				String info = msg.getMessage("RequestsController.processError." + action.toString(),
						((RequestEntry) item).request.getRequestId());

				throw new ControllerException(info, e);
			}
		}
		if (types.contains(RequestType.Registration))
		{
			bus.fireEvent(new RegistrationRequestsChangedEvent());
		}
		
		if (types.contains(RequestType.Enquiry))
		{
			bus.fireEvent(new EnquiryResponsesChangedEvent());
		}
		
	}

	private InternalRequestProcessingResponse processSingle(RequestEntry item, RegistrationRequestAction action, EventsBus bus)
			throws EngineException
	{
		UserRequestState<?> request = item.request;
		if (item.getType().equals(RequestType.Registration))
		{
			regMan.processRegistrationRequest(request.getRequestId(),
					(RegistrationRequest) request.getRequest(), action, null, null);
			return new InternalRequestProcessingResponse(RequestType.Registration);
		} else
		{
			enqMan.processEnquiryResponse(request.getRequestId(), (EnquiryResponse) request.getRequest(),
					action, null, null);
			return new InternalRequestProcessingResponse(RequestType.Registration);

		}
	}
	
	private static class InternalRequestProcessingResponse
	{
		final RequestType type;

		InternalRequestProcessingResponse(RequestType type)
		{
			this.type = type;
		}
	}

}
