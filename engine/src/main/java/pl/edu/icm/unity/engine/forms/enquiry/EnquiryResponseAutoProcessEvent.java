/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;

/**
 * Sent to trigger auto-processing of an enquiry response
 * @author K. Benedyczak
 */
public class EnquiryResponseAutoProcessEvent
{
	public final EnquiryForm form;
	public final EnquiryResponseState requestFull;
	public final String logMessageTemplate;

	public EnquiryResponseAutoProcessEvent(EnquiryForm form, EnquiryResponseState requestFull,
			String logMessageTemplate)
	{
		this.form = form;
		this.requestFull = requestFull;
		this.logMessageTemplate = logMessageTemplate;
	}
}
