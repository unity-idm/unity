/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.List;
import java.util.Objects;

/**
 * Contains configuration of group delegation
 * 
 * @author P.Piernik
 *
 */
public class GroupDelegationConfiguration
{
	private boolean enabled;
	private String logoUrl;
	private String registratioForm;
	private String signupEnquiryForm;
	private String stickyEnquiryForm;
	private List<String> attributes;

	/**
	 * Used by Jackson during deserialization
	 */
	public GroupDelegationConfiguration()
	{

	}

	public GroupDelegationConfiguration(boolean enabled)
	{
		this(enabled, null, null, null, null, null);
	}

	public GroupDelegationConfiguration(boolean enabled, String logoUrl, String registratioForm,
			String signupEnquiryForm, String stickyEnquiryForm, List<String> attributes)
	{
		this.enabled = enabled;
		this.logoUrl = logoUrl;
		this.registratioForm = registratioForm;
		this.signupEnquiryForm = signupEnquiryForm;
		this.stickyEnquiryForm = stickyEnquiryForm;
		this.attributes = attributes;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public String getLogoUrl()
	{
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl)
	{
		this.logoUrl = logoUrl;
	}

	public String getRegistratioForm()
	{
		return registratioForm;
	}

	public void setRegistratioForm(String registratioForm)
	{
		this.registratioForm = registratioForm;
	}

	public String getSignupEnquiryForm()
	{
		return signupEnquiryForm;
	}

	public void setSignupEnquiryForm(String signupEnquiryForm)
	{
		this.signupEnquiryForm = signupEnquiryForm;
	}

	public String getStickyEnquiryForm()
	{
		return stickyEnquiryForm;
	}

	public void setStickyEnquiryForm(String stickyEnquiryForm)
	{
		this.stickyEnquiryForm = stickyEnquiryForm;
	}

	public List<String> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<String> attributes)
	{
		this.attributes = attributes;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.enabled, this.logoUrl, this.registratioForm, this.signupEnquiryForm,
				this.stickyEnquiryForm, this.attributes);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final GroupDelegationConfiguration other = (GroupDelegationConfiguration) obj;
		return Objects.equals(this.enabled, other.enabled)
				&& Objects.equals(this.registratioForm, other.registratioForm)
				&& Objects.equals(this.signupEnquiryForm, other.signupEnquiryForm)
				&& Objects.equals(this.logoUrl, other.logoUrl)
				&& Objects.equals(this.stickyEnquiryForm, other.stickyEnquiryForm)
				&& Objects.equals(this.attributes, other.attributes);

	}
}
