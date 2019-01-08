/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

/**
 * Holds the information relevant for automatic invitation processing.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AutomaticInvitationProcessingParam
{
	private String formName;

	public AutomaticInvitationProcessingParam(String formName)
	{
		this.formName = formName;
	}

	public String getFormName()
	{
		return formName;
	}

	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((formName == null) ? 0 : formName.hashCode());
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
		AutomaticInvitationProcessingParam other = (AutomaticInvitationProcessingParam) obj;
		if (formName == null)
		{
			if (other.formName != null)
				return false;
		} else if (!formName.equals(other.formName))
			return false;
		return true;
	}
}
