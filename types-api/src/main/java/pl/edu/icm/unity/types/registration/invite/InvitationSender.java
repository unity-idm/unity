/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.registration.invite;

import pl.edu.icm.unity.exceptions.EngineException;

public interface InvitationSender
{
	void send(RegistrationInvitationParam registrationInvitationParam, String code) throws EngineException;

	void send(EnquiryInvitationParam enquiryInvitationParam, String code) throws EngineException;
}
