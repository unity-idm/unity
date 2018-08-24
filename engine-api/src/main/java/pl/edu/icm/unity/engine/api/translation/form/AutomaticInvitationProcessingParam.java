/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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
	public enum InvitationProcessingMode
	{
		APPLY_ALL_ATTRIBUTES, 
		APPLY_USER_EDITABLE_ATTRIBUTES
	}

	private String formName;
	private InvitationProcessingMode mode;

	public AutomaticInvitationProcessingParam(String formName, InvitationProcessingMode mode)
	{
		this.formName = formName;
		this.mode = mode;
	}

	public String getFormName()
	{
		return formName;
	}

	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	public InvitationProcessingMode getMode()
	{
		return mode;
	}

	public void setMode(InvitationProcessingMode mode)
	{
		this.mode = mode;
	}
}
