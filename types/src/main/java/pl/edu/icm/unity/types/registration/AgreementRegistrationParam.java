/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Defines an agreement to be shown.
 * @author K. Benedyczak
 */
public class AgreementRegistrationParam
{
	private String text;
	private boolean manatory;

	public String getText()
	{
		return text;
	}
	public void setText(String text)
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
}
