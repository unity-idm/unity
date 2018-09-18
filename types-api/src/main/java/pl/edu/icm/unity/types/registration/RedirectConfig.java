/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Objects;

import pl.edu.icm.unity.types.I18nString;

/**
 * Configuration of redirect that can happen in various cases of finishing of registration.
 * In future we may steer more details of redirect (e.g. is it automatic or after click).
 * @author K. Benedyczak
 */
public class RedirectConfig
{
	private I18nString redirectCaption;
	private boolean automatic;
	private String redirectURL;

	public RedirectConfig(I18nString redirectCaption, String redirectURL, boolean automatic)
	{
		this.redirectCaption = redirectCaption;
		this.redirectURL = redirectURL;
		this.automatic = automatic;
	}

	//for JSON
	protected RedirectConfig()
	{
	}
	
	public I18nString getRedirectCaption()
	{
		return redirectCaption;
	}
	public String getRedirectURL()
	{
		return redirectURL;
	}

	public boolean isAutomatic()
	{
		return automatic;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof RedirectConfig))
			return false;
		RedirectConfig castOther = (RedirectConfig) other;
		return Objects.equals(redirectCaption, castOther.redirectCaption)
				&& Objects.equals(automatic, castOther.automatic)
				&& Objects.equals(redirectURL, castOther.redirectURL);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(redirectCaption, automatic, redirectURL);
	}
}
