/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.registration.layout.FormLayout;

/**
 * Holds the information about primary and secondary layouts used in
 * registration form.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayouts
{
	private FormLayout primaryLayout;
	private FormLayout secondaryLayout;
	private boolean isLocalSignupEmbeddedAsButton;
	
	public RegistrationFormLayouts() {}
	
	@JsonCreator
	public RegistrationFormLayouts(ObjectNode json)
	{
		fromJson(json);
	}
	
	private void fromJson(ObjectNode root)
	{
		if (JsonUtil.notNull(root, "primaryLayout"))
		{
			setPrimaryLayout(new FormLayout((ObjectNode) root.get("primaryLayout")));
		}
		if (JsonUtil.notNull(root, "secondaryLayout"))
		{
			setSecondaryLayout(new FormLayout((ObjectNode) root.get("secondaryLayout")));
		}
		if (JsonUtil.notNull(root, "isLocalSignupEmbeddedAsButton"))
		{
			setLocalSignupEmbeddedAsButton(root.get("isLocalSignupEmbeddedAsButton").asBoolean());
		}
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		if (getPrimaryLayout() != null)
			root.set("primaryLayout", getPrimaryLayout().toJson());
		if (getSecondaryLayout() != null)
			root.set("secondaryLayout", getSecondaryLayout().toJson());
		root.put("isLocalSignupEmbeddedAsButton", isLocalSignupEmbeddedAsButton);
		return root;
	}
	
	public FormLayout getPrimaryLayout()
	{
		return primaryLayout;
	}

	public void setPrimaryLayout(FormLayout primaryLayout)
	{
		this.primaryLayout = primaryLayout;
	}

	public FormLayout getSecondaryLayout()
	{
		return secondaryLayout;
	}

	public void setSecondaryLayout(FormLayout secondaryLayout)
	{
		this.secondaryLayout = secondaryLayout;
	}
	
	public boolean isLocalSignupEmbeddedAsButton()
	{
		return isLocalSignupEmbeddedAsButton;
	}

	public void setLocalSignupEmbeddedAsButton(boolean isLocalSignupEmbeddedAsButton)
	{
		this.isLocalSignupEmbeddedAsButton = isLocalSignupEmbeddedAsButton;
	}

	public void validate(RegistrationForm registrationForm)
	{
		if (getPrimaryLayout() != null)
			FormLayoutUtils.validatePrimaryLayout(registrationForm);
		if (getSecondaryLayout() != null)
			FormLayoutUtils.validateSecondaryLayout(registrationForm);
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof RegistrationFormLayouts))
			return false;
		RegistrationFormLayouts castOther = (RegistrationFormLayouts) other;
		return Objects.equals(primaryLayout, castOther.primaryLayout)
				&& Objects.equals(secondaryLayout, castOther.secondaryLayout);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(primaryLayout, secondaryLayout);
	}
}
