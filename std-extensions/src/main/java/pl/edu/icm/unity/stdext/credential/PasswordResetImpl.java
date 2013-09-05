/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import org.bouncycastle.util.Arrays;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.stdext.utils.CryptoUtils;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Default implementation of {@link CredentialReset}. This implementation is stateful, i.e. from creation it
 * must be used exclusively by a single reset procedure.
 * @author K. Benedyczak
 */
public class PasswordResetImpl
{
	private NotificationProducer insecureNotificationsProducer;
	private AttributesManagement insecureAttributesManagement;
	private IdentitiesManagement insecureIdentitiesManagement;
	
	private EntityParam subject;
	private String credentialId;	
	private CredentialResetSettings settings;
	
	private String lastCodeSent;
	
	
	
	/**
	 * @return settings of credential reset
	 */
	public CredentialResetSettings getSettings()
	{
		return settings;
	}

	/**
	 * @return security question if it is defined, and settings of the credential do not require to 
	 * know the question. null otherwise.
	 */
	public String getSecurityQuestion()
	{
//		if (settings.isEnabled() && settings.isRequireSecurityQuestion())
//			return securityQuestion;
		return null;
	}

	/**
	 * Sends a random confirmation code to the receiver. The argument is used only if 
	 * {@link CredentialResetSettings} mandate to verify it - then it must be the same as the credential 
	 * configured. Otherwise it is ignored. 
	 * @param emailAddress See general description.
	 * @throws WrongArgumentException If the email address is incorrect
	 * @throws IllegalIdentityValueException if the identity has no email address defined.
	 * @throws IllegalCredentialException if maximum amount of code re-sends was reached
	 */
	public void sendCode(String emailAddress) throws WrongArgumentException,
			IllegalIdentityValueException, IllegalCredentialException
	{
		// TODO Auto-generated method stub
	}

	/**
	 * Changes the credential to a new one.  
	 * @param newCredential new credential, typically encoded in credential specific way.
	 * @param question the chosen security question. Relevant only if configuration requires its selection.
	 * @param questionAnswer an answer to the security question. Relevant only if configuration requires it.
	 * @param emailCode code retrieved by by e-mail. Relevant only if configuration requires it
	 * @throws IllegalCredentialException if the new credential is invalid, for instance the password is too weak.
	 * @throws WrongArgumentException if the code, question or answer is invalid
	 */
	public void resetPassword(String newCredential, String question, String questionAnswer,
			String emailCode) throws IllegalCredentialException, WrongArgumentException
	{
//		if (!settings.isEnabled())
//			throw new IllegalCredentialException("Credential reset is not enabled for this credential");
//		if (tries >= settings.getMaxTries())
//			throw new IllegalCredentialException("Credential reset is is blocked for this credential");
//		
//		if (settings.getRequireSecurityQuestion() == ConfirmationMode.YES_WITH_QUESTION)
//		{
//			if (!securityQuestion.equals(question))
//				throw new WrongArgumentException("Confirmation data is invalid");
//		}
//		
//		if (settings.getRequireSecurityQuestion() != ConfirmationMode.NO)
//		{
//			byte[] hash = CryptoUtils.hash(questionAnswer.toLowerCase(), question);
//			if (!Arrays.areEqual(hash, answerHash))
//				throw new WrongArgumentException("Confirmation data is invalid");
//		}
//		
//		if (settings.getRequireEmailConfirmation() != ConfirmationMode.NO)
//		{
//			if (lastCodeSent == null)
//				throw new IllegalStateException("Security code was not yet sent");
//			if (!lastCodeSent.equals(emailCode))
//				throw new WrongArgumentException("Confirmation data is invalid");
//		}
		
		
		// TODO Auto-generated method stub
	}
}
