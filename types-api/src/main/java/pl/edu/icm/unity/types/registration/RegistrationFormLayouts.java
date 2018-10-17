/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Objects;

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
