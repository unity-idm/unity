/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Represents information provided by admin, which authentication flow is
 * supported/should be displayed on the registration form as an option to fill
 * it out.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AuthenticationFlowsSpec
{
	/**
	 * Controls the flow of automatic form processing after successful authN.
	 */
	public enum AutomaticFormProcessingAfterAuthnSettings
	{
		/**
		 * Form is handled in default way - user needs to fill it out and click OK
		 * to submit the form.
		 */
		DISABLED, 
		/**
		 * When remote authn context contains all values from the form, the
		 * registration is submitted automatically. Whenever there is one or more
		 * registration form value which is empty (meaning no value matched from
		 * retrieved context) the form is displayed and user needs to fill it out
		 * and submit manually.
		 */
		SUBMIT_WHEN_ALL_PROVIDED
	}
	
	private Set<String> specs;
	/**
	 * @implNote: shall the registration form, filled out with attributes
	 *            from authN result, be available for user to edit.
	 */
	private AutomaticFormProcessingAfterAuthnSettings automaticProcessing;

	
	AuthenticationFlowsSpec(Set<String> specs, AutomaticFormProcessingAfterAuthnSettings editAfterAuthn)
	{
		this.specs = specs;
		this.automaticProcessing = editAfterAuthn;
	}
	
	AuthenticationFlowsSpec()
	{
		specs = new HashSet<>();
		automaticProcessing = AutomaticFormProcessingAfterAuthnSettings.DISABLED;
	}
	
	@JsonCreator
	public AuthenticationFlowsSpec(ObjectNode json)
	{
		fromJson(json);
	}

	private void fromJson(ObjectNode root)
	{
		try
		{
			this.automaticProcessing = AutomaticFormProcessingAfterAuthnSettings.valueOf(root.get("automaticProcessing").asText());
			ArrayNode specsNode = (ArrayNode) root.get("specs");
			specs = new HashSet<>(specsNode.size());
			for (int i=0; i<specsNode.size(); i++)
			{
				specs.add(specsNode.get(i).asText());
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize authentication flows spec from JSON", e);
		}
	}
	
	@JsonValue
	public ObjectNode toJsonObject()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("automaticProcessing", automaticProcessing.name());
		ArrayNode jsonSpecs = root.putArray("specs");
		specs.forEach(spec -> jsonSpecs.add(spec));
		return root;
	}
	
	public Set<String> getSpecs()
	{
		return specs;
	}

	public void setSpecs(Set<String> specs)
	{
		this.specs = specs;
	}

	public AutomaticFormProcessingAfterAuthnSettings getAutomaticProcessing()
	{
		return automaticProcessing;
	}

	public void setAutomaticProcessing(AutomaticFormProcessingAfterAuthnSettings automaticProcessing)
	{
		this.automaticProcessing = automaticProcessing;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((automaticProcessing == null) ? 0 : automaticProcessing.hashCode());
		result = prime * result + ((specs == null) ? 0 : specs.hashCode());
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
		AuthenticationFlowsSpec other = (AuthenticationFlowsSpec) obj;
		if (automaticProcessing != other.automaticProcessing)
			return false;
		if (specs == null)
		{
			if (other.specs != null)
				return false;
		} else if (!specs.equals(other.specs))
			return false;
		return true;
	}
	
	public static AuthenticationFlowsSpecBuilder builder()
	{
		return new AuthenticationFlowsSpecBuilder();
	}
	
	public static class AuthenticationFlowsSpecBuilder
	{
		private Set<String> specs = new HashSet<>();
		private AutomaticFormProcessingAfterAuthnSettings editAfterAuthn;
		
		public AuthenticationFlowsSpecBuilder withEditAfterAuthnForm(AutomaticFormProcessingAfterAuthnSettings editAfterAuthn)
		{
			this.editAfterAuthn = editAfterAuthn;
			return this;
		}
		
		public AuthenticationFlowsSpecBuilder withSpecs(Set<String> specs)
		{
			this.specs = new HashSet<>(specs);
			return this;
		}
		
		public AuthenticationFlowsSpec build()
		{
			return new AuthenticationFlowsSpec(specs, editAfterAuthn);
		}
	}
}
