/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

/**
 * Complete information on what to show on the final screen after completed registration (either failed or successful).
 * @author K. Benedyczak
 */
public class FinalRegistrationConfiguration
{
	public final String mainInformation;
	public final String extraInformation;
	public final Runnable redirectHandler;
	public final String redirectButtonText;
	
	public FinalRegistrationConfiguration(String mainInformation,
			String extraInformation, Runnable redirectHandler, String redirectButtonText)
	{
		this.mainInformation = mainInformation;
		this.extraInformation = extraInformation;
		this.redirectHandler = redirectHandler;
		this.redirectButtonText = redirectButtonText;
	}
}
