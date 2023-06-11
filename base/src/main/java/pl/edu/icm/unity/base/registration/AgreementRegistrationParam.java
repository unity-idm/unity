/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import pl.edu.icm.unity.base.i18n.I18nString;

/**
 * Defines an agreement to be shown.
 * @author K. Benedyczak
 */
public class AgreementRegistrationParam
{
	private I18nString text;
	private boolean manatory;

	
	public AgreementRegistrationParam(I18nString text, boolean manatory)
	{
		this.text = text;
		this.manatory = manatory;
	}
	
	public AgreementRegistrationParam()
	{
	}

	public I18nString getText()
	{
		return text;
	}
	public void setText(I18nString text)
	{
		this.text = text;
	}
	public boolean isManatory()
	{
		return manatory;
	}
	public void setManatory(boolean manatory)
	{
		this.manatory = manatory;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (manatory ? 1231 : 1237);
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		AgreementRegistrationParam other = (AgreementRegistrationParam) obj;
		if (manatory != other.manatory)
			return false;
		if (text == null)
		{
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
}
