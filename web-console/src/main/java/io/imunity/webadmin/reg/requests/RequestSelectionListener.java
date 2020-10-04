/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webadmin.reg.requests;

import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

public interface RequestSelectionListener
{
	void registrationChanged(RegistrationRequestState request);

	void enquiryChanged(EnquiryResponseState request);

	void deselected();
}