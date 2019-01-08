/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Optional;

/**
 * Stores information about registration options enabled for the endpoint.
 * 
 * @author K. Benedyczak
 */
public class EndpointRegistrationConfiguration
{
	private List<String> enabledForms;
	private Optional<String> externalRegistrationURL;
	private boolean showRegistrationOption;
	private boolean displayRegistrationFormsInHeader;

	public EndpointRegistrationConfiguration(boolean showRegistrationOption)
	{
		this.showRegistrationOption = showRegistrationOption;
	}

	public EndpointRegistrationConfiguration(List<String> enabledForms, boolean showRegistrationOption,
			boolean displayRegistrationFormsInHeader, Optional<String> externalRegistrationURL)
	{
		this.enabledForms = enabledForms;
		this.showRegistrationOption = showRegistrationOption;
		this.displayRegistrationFormsInHeader = displayRegistrationFormsInHeader;
		this.externalRegistrationURL = externalRegistrationURL;
	}

	public List<String> getEnabledForms()
	{
		return enabledForms;
	}

	public boolean isShowRegistrationOption()
	{
		return showRegistrationOption;
	}

	public boolean isDisplayRegistrationFormsInHeader()
	{
		return displayRegistrationFormsInHeader;
	}

	public Optional<String> getExternalRegistrationURL()
	{
		return externalRegistrationURL;
	}
}
