/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;

/**
 * Stores information about registration options enabled for the endpoint.
 * @author K. Benedyczak
 */
public class EndpointRegistrationConfiguration
{
	private List<String> enabledForms;
	private boolean showRegistrationOption;

	public EndpointRegistrationConfiguration(List<String> enabledForms,
			boolean showRegistrationOption)
	{
		this.enabledForms = enabledForms;
		this.showRegistrationOption = showRegistrationOption;
	}

	public List<String> getEnabledForms()
	{
		return enabledForms;
	}

	public void setEnabledForms(List<String> enabledForms)
	{
		this.enabledForms = enabledForms;
	}

	public boolean isShowRegistrationOption()
	{
		return showRegistrationOption;
	}

	public void setShowRegistrationOption(boolean showRegistrationOption)
	{
		this.showRegistrationOption = showRegistrationOption;
	}
}
