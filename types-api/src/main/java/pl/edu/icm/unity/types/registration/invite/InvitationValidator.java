/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.registration.invite;

import pl.edu.icm.unity.exceptions.EngineException;

public interface InvitationValidator
{
	void validateUpdate(RegistrationInvitationParam current, InvitationParam toUpdate) throws EngineException;

	void validateUpdate(EnquiryInvitationParam current, InvitationParam toUpdate) throws EngineException;

	void validateUpdate(ComboInvitationParam current, InvitationParam toUpdate) throws EngineException;

	void validate(RegistrationInvitationParam invitationParam) throws EngineException;

	void validate(EnquiryInvitationParam invitationParam) throws EngineException;

	void validate(ComboInvitationParam invitationParam) throws EngineException;

}
