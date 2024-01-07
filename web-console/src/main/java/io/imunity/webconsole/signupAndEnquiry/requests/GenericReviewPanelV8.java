/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.requests;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Component showing either {@link RegistrationReviewPanelV8} or {@link EnquiryReviewPanelV8} depending on an input to
 * show.
 * @author K. Benedyczak
 */
@PrototypeComponent
class GenericReviewPanelV8 extends CustomComponent
{
	private EnquiryReviewPanelV8 enquiryPanel;
	private RegistrationReviewPanelV8 registrationPanel;

	@Autowired
	GenericReviewPanelV8(EnquiryReviewPanelV8 enquiryPanel,
			RegistrationReviewPanelV8 registrationPanel)
	{
		this.enquiryPanel = enquiryPanel;
		this.registrationPanel = registrationPanel;
	}

	void setEnquiry(EnquiryResponseState requestState, EnquiryForm form)
	{
		enquiryPanel.setInput(requestState, form);
		setCompositionRoot(enquiryPanel);
	}
	
	void setRegistration(RegistrationRequestState requestState, RegistrationForm form)
	{
		registrationPanel.setInput(requestState, form);
		setCompositionRoot(registrationPanel);
	}
	
	RegistrationRequest getUpdatedRequest()
	{
		return registrationPanel.getUpdatedRequest();
	}
	
	EnquiryResponse getUpdatedResponse()
	{
		return enquiryPanel.getUpdatedRequest();
	}
	
}
