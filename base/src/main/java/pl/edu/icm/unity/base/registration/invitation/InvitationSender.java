/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.registration.invitation;

import pl.edu.icm.unity.base.exceptions.EngineException;

public interface InvitationSender
{
	void send(RegistrationInvitationParam registrationInvitationParam, String code) throws EngineException;

	void send(EnquiryInvitationParam enquiryInvitationParam, String code) throws EngineException;
}
