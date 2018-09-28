/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Support for rewriting confirmation tokens of registration form (upon its acceptance) to regular confirmation tokens
 * of an existing entity. 
 * <p>
 * Implementation note: this class is separate from {@link RegistrationConfirmationSupport} 
 * to eliminate dependency cycles.
 *   
 * @author K. Benedyczak
 */
@Component
public class RegistrationConfirmationRewriteSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RegistrationConfirmationRewriteSupport.class);
	
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private AttributeTypeHelper attributeTypeHelper;
	
	/**
	 * Searches for all confirmation tokens bound to the request (all present are not yet confirmed). Those tokens
	 * are rewritten to be bound to a given entity. This method is useful after a request is accepted and 
	 * confirmation should effect in confirming element of existing entity instead of internal request state. 
	 * @param finalRequest
	 * @param entityId
	 * @throws EngineException
	 */
	public void rewriteRequestToken(UserRequestState<?> finalRequest, long entityId) 
			throws EngineException
	{
		List<Token> tks = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE);
		for (Token tk : tks)
		{
			RegistrationEmailConfirmationState state;
			try
			{
				state = new RegistrationEmailConfirmationState(tk.getContentsString());
			} catch (IllegalArgumentException e)
			{
				//OK - not a registration token
				continue;
			}
			if (state.getRequestId().equals(finalRequest.getRequestId()))
			{
				if (state.getFacilityId().equals(
						RegistrationReqEmailAttribiuteConfirmationState.FACILITY_ID))
				{
					rewriteSingleAttributeToken(finalRequest, tk, entityId);
				} else if (state.getFacilityId().equals(
						RegistrationReqEmailIdentityConfirmationState.FACILITY_ID))
				{
					rewriteSingleIdentityToken(finalRequest, tk, entityId);
				}
			}
		}
	}

	private void rewriteSingleAttributeToken(UserRequestState<?> request, Token tk, 
			long entityId) throws EngineException
	{

		RegistrationReqEmailAttribiuteConfirmationState oldState = new RegistrationReqEmailAttribiuteConfirmationState(
				new String(tk.getContents(), StandardCharsets.UTF_8));
		boolean inRequest = false;
		for (Attribute attribute : request.getRequest().getAttributes())
		{
			if (attribute == null || attribute.getValueSyntax() == null)
				continue;
			if (inRequest)
				break;
			AttributeValueSyntax<?> syntax = attributeTypeHelper.getUnconfiguredSyntax(
					attribute.getValueSyntax());
			if (syntax.isEmailVerifiable()
					&& attribute.getName().equals(oldState.getType())
					&& attribute.getValues() != null)
			{
				for (String o : attribute.getValues())
				{
					Object domainValue = syntax.convertFromString(o);
					VerifiableElement val = (VerifiableElement) domainValue;
					if (val.getValue().equals(oldState.getValue()))
					{
						inRequest = true;
						break;
					}
				}
			}
		}
		tokensMan.removeToken(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		if (inRequest)
		{
			EmailAttribiuteConfirmationState newstate = new EmailAttribiuteConfirmationState(
					entityId, oldState.getType(), oldState.getValue(),
					oldState.getLocale(), oldState.getGroup());
			log.debug("Update confirmation token " + tk.getValue()
					+ ", changing facility to " + newstate.getFacilityId());
			tokensMan.addToken(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk
					.getValue(), newstate.getSerializedConfiguration()
					.getBytes(StandardCharsets.UTF_8), tk.getCreated(), tk
					.getExpires());
		}
	}

	
	private void rewriteSingleIdentityToken(UserRequestState<?> request, Token tk, 
			long entityId) throws EngineException
	{
		RegistrationReqEmailIdentityConfirmationState oldState = new RegistrationReqEmailIdentityConfirmationState(
				new String(tk.getContents(), StandardCharsets.UTF_8));
		boolean inRequest = false;
		for (IdentityParam id : request.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			
			if (id.getTypeId().equals(oldState.getType())
					&& id.getValue().equals(oldState.getValue()))
			{
				inRequest = true;
				break;
			}
		}

		tokensMan.removeToken(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		if (inRequest)
		{
			EmailIdentityConfirmationState newstate = new EmailIdentityConfirmationState(
					entityId, oldState.getType(), oldState.getValue(),
					oldState.getLocale());
			log.debug("Update confirmation token " + tk.getValue()
					+ ", changing facility to " + newstate.getFacilityId());
			tokensMan.addToken(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk
					.getValue(), newstate.getSerializedConfiguration()
					.getBytes(StandardCharsets.UTF_8), tk.getCreated(), tk
					.getExpires());
		}

	}
}
