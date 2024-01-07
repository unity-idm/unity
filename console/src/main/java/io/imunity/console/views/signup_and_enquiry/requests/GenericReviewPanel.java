/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.requests;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Component showing either {@link RegistrationReviewPanel} or {@link EnquiryReviewPanel} depending on an input to
 * show.
 * @author K. Benedyczak
 */
@PrototypeComponent
class GenericReviewPanel extends VerticalLayout
{
	private EnquiryReviewPanel enquiryPanel;
	private RegistrationReviewPanel registrationPanel;

	@Autowired
	GenericReviewPanel(EnquiryReviewPanel enquiryPanel,
			RegistrationReviewPanel registrationPanel)
	{
		this.enquiryPanel = enquiryPanel;
		this.registrationPanel = registrationPanel;
		
		setPadding(false);
		setMargin(false);
		setSpacing(false);
	}

	void setEnquiry(EnquiryResponseState requestState, EnquiryForm form)
	{
		enquiryPanel.setInput(requestState, form);
		removeAll();
		add(enquiryPanel);
	}
	
	void setRegistration(RegistrationRequestState requestState, RegistrationForm form)
	{
		registrationPanel.setInput(requestState, form);
		removeAll();
		add(registrationPanel);
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
