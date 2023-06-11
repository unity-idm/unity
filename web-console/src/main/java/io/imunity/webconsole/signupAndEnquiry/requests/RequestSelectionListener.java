/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.requests;

import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;

public interface RequestSelectionListener
{
	void registrationChanged(RegistrationRequestState request);

	void enquiryChanged(EnquiryResponseState request);

	void deselected();
}