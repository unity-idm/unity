/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import java.util.Objects;

/**
 * Identity registration option.
 * @author K. Benedyczak
 */
public class IdentityRegistrationParam extends OptionalRegistrationParam
{
	private String identityType;
	private ConfirmationMode confirmationMode = ConfirmationMode.ON_SUBMIT;
	private URLQueryPrefillConfig urlQueryPrefill;
	
	public String getIdentityType()
	{
		return identityType;
	}

	public void setIdentityType(String identityType)
	{
		this.identityType = identityType;
	}

	public ConfirmationMode getConfirmationMode()
	{
		return confirmationMode;
	}

	public void setConfirmationMode(ConfirmationMode confirmationMode)
	{
		this.confirmationMode = confirmationMode;
	}

	public URLQueryPrefillConfig getUrlQueryPrefill()
	{
		return urlQueryPrefill;
	}

	public void setUrlQueryPrefill(URLQueryPrefillConfig urlQueryPrefill)
	{
		this.urlQueryPrefill = urlQueryPrefill;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(confirmationMode, identityType, urlQueryPrefill);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentityRegistrationParam other = (IdentityRegistrationParam) obj;
		return confirmationMode == other.confirmationMode && Objects.equals(identityType, other.identityType)
				&& Objects.equals(urlQueryPrefill, other.urlQueryPrefill);
	}
}
