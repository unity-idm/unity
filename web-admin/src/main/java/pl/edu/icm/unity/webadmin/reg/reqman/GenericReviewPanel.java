/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.ui.CustomComponent;

/**
 * Component showing either {@link RegistrationReviewPanel} or {@link EnquiryReviewPanel} depending on an input to
 * show.
 * @author K. Benedyczak
 */
public class GenericReviewPanel extends CustomComponent
{
	private EnquiryReviewPanel enquiryPanel;
	private RegistrationReviewPanel registrationPanel;
	
	public GenericReviewPanel(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentitiesManagement identitiesManagement)
	{
		this.enquiryPanel = new EnquiryReviewPanel(msg, handlersRegistry, idTypesRegistry, identitiesManagement);
		this.registrationPanel = new RegistrationReviewPanel(msg, handlersRegistry, idTypesRegistry);
	}

	public void setEnquiry(EnquiryResponseState requestState, EnquiryForm form)
	{
		enquiryPanel.setInput(requestState, form);
		setCompositionRoot(enquiryPanel);
	}
	
	public void setRegistration(RegistrationRequestState requestState, RegistrationForm form)
	{
		registrationPanel.setInput(requestState, form);
		setCompositionRoot(registrationPanel);
	}
	
	public RegistrationRequest getUpdatedRequest()
	{
		return registrationPanel.getUpdatedRequest();
	}
	
	public EnquiryResponse getUpdatedResponse()
	{
		return enquiryPanel.getUpdatedRequest();
	}
	
}
