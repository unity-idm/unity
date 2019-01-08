/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Invitations to fill registration form management
 * @author K. Benedyczak
 */
public interface InvitationManagement
{
	/**
	 * Associates a new invitation with a form. The invitation code is auto generated and returned
	 * @param invitation invitation to be added
	 * @return code assigned to the invitation
	 */
	String addInvitation(InvitationParam invitation) throws EngineException;

	/**
	 * Updates existing invitation. The email address and registration form can not be changed.
	 */
	void updateInvitation(String code, InvitationParam invitation) throws EngineException;
	
	/**
	 * Sends an invitation message to the invitation specified by the code. In case when there is 
	 * no such invitation, it has missing or invalid contact address or when the associated form has no message
	 * template for invitation this method throws exception. 
	 * @param code
	 */
	void sendInvitation(String code) throws EngineException;
	
	/**
	 * Removes a single invitation
	 * @param code
	 * @throws EngineException
	 */
	void removeInvitation(String code) throws EngineException;
	
	/**
	 * @return a list with all invitations
	 * @throws EngineException
	 */
	List<InvitationWithCode> getInvitations() throws EngineException;
	
	/**
	 * Retrieves an invitation by code
	 * @param code invitation code
	 * @return an invitation with the given code. Note that the returned invitation may happen to be expired.
	 * @throws EngineException More specifically {@link WrongArgumentException} is thrown when 
	 * there is no invitation with such code.
	 */
	InvitationWithCode getInvitation(String code) throws EngineException;
}
