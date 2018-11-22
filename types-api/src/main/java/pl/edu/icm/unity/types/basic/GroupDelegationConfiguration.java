/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;

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

	@JsonCreator
	public GroupDelegationConfiguration(ObjectNode root)
	{
		fromJson(root);
	}

	private void fromJson(ObjectNode root)
	{
		if (JsonUtil.notNull(root, "enabled"))
			setEnabled(root.get("enabled").asBoolean());
		if (JsonUtil.notNull(root, "logoUrl"))
			setLogoUrl(root.get("logoUrl").asText());
		if (JsonUtil.notNull(root, "registratioForm"))
			setRegistratioForm(root.get("registratioForm").asText());
		if (JsonUtil.notNull(root, "signupEnquiryForm"))
			setSignupEnquiryForm(root.get("signupEnquiryForm").asText());
		if (JsonUtil.notNull(root, "stickyEnquiryForm"))
			setStickyEnquiryForm(root.get("stickyEnquiryForm").asText());
		if (JsonUtil.notNull(root, "attributes"))
		{
			ArrayNode jsonAttrs = (ArrayNode) root.get("attributes");
			List<String> attrs = new ArrayList<>();
			for (JsonNode e : jsonAttrs)
				attrs.add(e.asText());
			setAttributes(attrs);
		}
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("enabled", isEnabled());
		root.put("logoUrl", getLogoUrl());
		root.put("registratioForm", getRegistratioForm());
		root.put("signupEnquiryForm", getSignupEnquiryForm());
		root.put("stickyEnquiryForm", getSignupEnquiryForm());
		ArrayNode attrs = root.putArray("attributes");
		List<String> attributes = getAttributes();
		if (attributes != null)
		{
			for (String attr : getAttributes())
				attrs.add(attr);
		}
		return root;
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
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		GroupDelegationConfiguration other = (GroupDelegationConfiguration) obj;
		if (enabled != other.enabled)
			return false;

		if (registratioForm == null)
		{
			if (other.registratioForm != null)
				return false;
		} else if (!registratioForm.equals(other.registratioForm))
			return false;

		if (signupEnquiryForm == null)
		{
			if (other.signupEnquiryForm != null)
				return false;
		} else if (!signupEnquiryForm.equals(other.signupEnquiryForm))
			return false;

		if (stickyEnquiryForm == null)
		{
			if (other.stickyEnquiryForm != null)
				return false;
		} else if (!stickyEnquiryForm.equals(other.stickyEnquiryForm))
			return false;

		if (logoUrl == null)
		{
			if (other.logoUrl != null)
				return false;
		} else if (!logoUrl.equals(other.logoUrl))
			return false;

		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;

		return true;
	}
}
