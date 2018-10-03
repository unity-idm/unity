/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;

/**
 * Describes registration context, i.e. circumstances and environment at the request submission.
 * This data influences the submitted request's processing.
 * @author K. Benedyczak
 */
public class RegistrationContext
{
	/**
	 * Describes how the registration form was triggered.
	 * @author K. Benedyczak
	 */
	public enum TriggeringMode {
		/**
		 * User selected registration manually on one of login screens
		 */
		manualAtLogin, 
		
		/**
		 * User entered a well-known registration form link
		 */
		manualStandalone, 

		/**
		 * The form is being filled from the AdminUI
		 */
		manualAdmin, 
		
		/**
		 * Form was shown after a successful remote authentication 
		 * which was not mapped to a local entity by an input translation profile. 
		 */
		afterRemoteLoginWhenUnknownUser,
		
		/**
		 * The form was submitted as a result of secondary sign up step, where a
		 * remote authentication was used to fill out registration form.
		 */
		afterRemoteLoginFromRegistrationForm
	}
	
	public final boolean isOnIdpEndpoint;
	public final TriggeringMode triggeringMode;
	
	public RegistrationContext(boolean isOnIdpEndpoint,
			TriggeringMode triggeringMode)
	{
		this.isOnIdpEndpoint = isOnIdpEndpoint;
		this.triggeringMode = triggeringMode;
	}
	
	public RegistrationContext(JsonNode object)
	{
		isOnIdpEndpoint = object.get("isOnIdpEndpoint").asBoolean();
		triggeringMode = TriggeringMode.valueOf(object.get("triggeringMode").asText());
	}
	
	@JsonValue
	public JsonNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("isOnIdpEndpoint", isOnIdpEndpoint);
		root.put("triggeringMode", triggeringMode.name());
		return root;
	}

	
	@Override
	public String toString()
	{
		return "RegistrationContext [isOnIdpEndpoint="	+ isOnIdpEndpoint 
				+ ", triggeringMode=" + triggeringMode + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (isOnIdpEndpoint ? 1231 : 1237);
		result = prime * result
				+ ((triggeringMode == null) ? 0 : triggeringMode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegistrationContext other = (RegistrationContext) obj;
		if (isOnIdpEndpoint != other.isOnIdpEndpoint)
			return false;
		if (triggeringMode != other.triggeringMode)
			return false;
		return true;
	}
}
