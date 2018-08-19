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
	 * Controls the flow of automatic form filling after successful authN.
	 */
	public enum EditAfterAuthnSettings
	{
		/**
		 * Whatever comes from the external system, is mapped to registration form.
		 * User does not have a possibility to update retrieved values.
		 */
		NEVER, 
		/**
		 * User is asked to confirm/edit the values that comes from external system.
		 */
		ALWAYS,
		/**
		 * Whenever the value which is mapped to a registration form property is empty,
		 * user is prompted to enter the missing value & confirm existing once.
		 */
		INTERACTIVE
	}
	
	private Set<String> specs;
	/**
	 * @implNote: shall the registration form, filled out with attributes
	 *            from authN result, be available for user to edit.
	 */
	private EditAfterAuthnSettings editAfterAuthn;

	
	AuthenticationFlowsSpec(Set<String> specs, EditAfterAuthnSettings editAfterAuthn)
	{
		this.specs = specs;
		this.editAfterAuthn = editAfterAuthn;
	}
	
	AuthenticationFlowsSpec()
	{
		specs = new HashSet<>();
		editAfterAuthn = EditAfterAuthnSettings.NEVER;
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
			this.editAfterAuthn = EditAfterAuthnSettings.valueOf(root.get("editAfterAuthn").asText());
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
		root.put("editAfterAuthn", editAfterAuthn.name());
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

	public EditAfterAuthnSettings isEditAfterAuthn()
	{
		return editAfterAuthn;
	}

	public void setEditAfterAuthn(EditAfterAuthnSettings editAfterAuthn)
	{
		this.editAfterAuthn = editAfterAuthn;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((editAfterAuthn == null) ? 0 : editAfterAuthn.hashCode());
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
		if (editAfterAuthn != other.editAfterAuthn)
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
		private EditAfterAuthnSettings editAfterAuthn;
		
		public AuthenticationFlowsSpecBuilder withEditAfterAuthnForm(EditAfterAuthnSettings editAfterAuthn)
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
