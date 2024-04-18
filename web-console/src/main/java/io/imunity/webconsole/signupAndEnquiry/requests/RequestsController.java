/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.requests;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.registration.RequestType;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.basic.*;
import pl.edu.icm.unity.types.registration.*;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponsesChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestsChangedEvent;

import static java.util.stream.Collectors.toSet;

/**
 * Controller for all registration and enquiry request views.
 * 
 * @author P.Piernik
 *
 */
@Component
class RequestsController
{
	private final EntityManagement idMan;
	private final RegistrationsManagement regMan;
	private final EnquiryManagement enqMan;
	private final MessageSource msg;

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
			regMan.getRegistrationRequests().forEach(r -> res.add(new RequestEntry(r, msg, getRegisteredIdentity(r))));
			res.addAll(prepareEnquiryResponsesForPresentation());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("RequestsController.getAllError"), e);
		}
		return res;
	}

	private List<RequestEntry> prepareEnquiryResponsesForPresentation() throws EngineException
	{
		List<EnquiryResponseState> enquiryResponses = enqMan.getEnquiryResponses();
		Set<Long> entityIds = enquiryResponses.stream().map(EnquiryResponseState::getEntityId).collect(toSet());
		Map<Long, List<Identity>> idToEntity = idMan.getIdentitiesForEntities(entityIds);

		return enquiryResponses.stream()
				.map(r -> new RequestEntry(r, msg, getEnquiryIdentity(r, idToEntity)))
				.collect(Collectors.toList());
	}

	private static String getRegisteredIdentity(UserRequestState<?> request)
	{
		List<IdentityParam> identities = request.getRequest().getIdentities();
		if (identities.isEmpty())
			return "-";
		IdentityParam id = identities.get(0);
		return id == null ? "-" : id.toHumanReadableString();
	}

	private String getEnquiryIdentity(EnquiryResponseState enqRequest, Map<Long, List<Identity>> idToEntity)
	{
		List<Identity> identities = idToEntity.get(enqRequest.getEntityId());
		if (identities == null)
			return "-";
		List<IdentityParam> identityParams = identities.stream().map(i -> (IdentityParam) i)
				.collect(Collectors.toList());
		return getEmailIdentity(identityParams)
				.orElse(identities.isEmpty() ? "-" : identities.get(0).toHumanReadableString());
	}

	private Optional<String> getEmailIdentity(List<IdentityParam> identities)
	{
		for (IdentityParam id : identities)
		{
			if (id != null && id.getTypeId().equals(EmailIdentity.ID))
				return Optional.of(id.getValue());
		}
		return Optional.empty();
	}

	public void process(Collection<?> items, RegistrationRequestAction action, EventsBus bus)
			throws ControllerException
	{
		
		Set<RequestType> types = new HashSet<>();
		for (Object item : items)
		{
			try
			{
				types.add(processSingle((RequestEntry) item, action).type);
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

	private InternalRequestProcessingResponse processSingle(RequestEntry item, RegistrationRequestAction action)
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
