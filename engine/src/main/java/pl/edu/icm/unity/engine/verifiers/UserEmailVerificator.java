/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.verifiers;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.print.attribute.standard.DateTimeAtCompleted;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

/**
 * Email verification helper class.
 * 
 * @author P. Piernik
 */
@Component
public class UserEmailVerificator
{
	private final String EMAIL_CONFIRM_TOKEN_TYPE = "emailConfirmation";
	private TokensManagement tokensMan;
	private MessageTemplateManagement templateMan;
	
	@Autowired
	public UserEmailVerificator(TokensManagement tokensMan, MessageTemplateManagement templateMan)
	{
		this.tokensMan = tokensMan;
		this.templateMan = templateMan;
	}

	public void sendConfirmation(String addrress, String templateId, ConfirmationState state)
	{
		Date createDate = new Date();
		Calendar cl = Calendar. getInstance();
		cl.setTime(createDate);
		cl.add(Calendar.HOUR, 48);
		Date expires = cl.getTime();
		
		try
		{
			tokensMan.addToken(EMAIL_CONFIRM_TOKEN_TYPE, UUID.randomUUID().toString(), null , state.getSerializedConfiguration().getBytes() , createDate, expires);
		} catch (WrongArgumentException | IllegalIdentityValueException
				| IllegalTypeException | InternalException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//generate token
		//send email
		try
		{
			templateMan.getTemplate(templateId);
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
