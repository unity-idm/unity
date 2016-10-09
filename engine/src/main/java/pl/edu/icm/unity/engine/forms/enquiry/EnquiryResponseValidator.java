/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.forms.BaseRequestValidator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;

/**
 * Helper component with methods to validate {@link EnquiryResponse}.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseValidator extends BaseRequestValidator
{
	public void validateSubmittedResponse(EnquiryForm form, EnquiryResponse response,
			boolean doCredentialCheckAndUpdate) throws IllegalFormContentsException
	{
		super.validateSubmittedRequest(form, response, doCredentialCheckAndUpdate);
	}

	public void validateTranslatedRequest(EnquiryForm form, EnquiryResponse response, 
			TranslatedRegistrationRequest request) throws EngineException
	{
		validateFinalAttributes(request.getAttributes());
		validateFinalCredentials(response.getCredentials());
		validateFinalIdentities(request.getIdentities());
	}
}
