/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.home.console;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * Contains home service configuration
 * 
 * @author P.Piernik
 *
 */
public class HomeServiceConfiguration
{
	private List<String> enabledTabs;
	private List<String> enabledUserDetailsControls;
	private List<ExposedAttribute> exposedAttributes;
	private boolean allowRemovalSheduling;
	private boolean allow2ndFactorOptIn;
	private RemovalModes removalMode;
	private boolean enableUpMan;
	private String upManService;
	private String imageAttribute;
	private List<String> enquiryForms;

	HomeServiceConfiguration()
	{
		enabledTabs = new ArrayList<>();
		enabledUserDetailsControls = new ArrayList<>();
		exposedAttributes = new ArrayList<>();
		allowRemovalSheduling = true;
		allow2ndFactorOptIn = false;
		removalMode = RemovalModes.remove;
		enquiryForms = new ArrayList<>();
	}

	public String toProperties()
	{
		Properties raw = new Properties();
		List<String> allComponents = Stream.of(HomeEndpointProperties.Components.values())
				.map(HomeEndpointProperties.Components::toString).collect(Collectors.toList());
	
		allComponents.removeAll(enabledTabs);
		allComponents.removeAll(enabledUserDetailsControls);

		if (!allComponents.isEmpty())
		{
			allComponents.forEach(c -> raw.put(HomeEndpointProperties.PREFIX
					+ HomeEndpointProperties.DISABLED_COMPONENTS
					+ (allComponents.indexOf(c) + 1), c));
		}

		for (ExposedAttribute a : exposedAttributes)
		{
			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
					+ (exposedAttributes.indexOf(a) + 1) + "."
					+ HomeEndpointProperties.GWA_ATTRIBUTE, a.getName());
			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
					+ (exposedAttributes.indexOf(a) + 1) + "."
					+ HomeEndpointProperties.GWA_GROUP, a.getGroup().group.toString());

			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
					+ (exposedAttributes.indexOf(a) + 1) + "."
					+ HomeEndpointProperties.GWA_EDITABLE, String.valueOf(a.isEditable()));

			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
					+ (exposedAttributes.indexOf(a) + 1) + "."
					+ HomeEndpointProperties.GWA_SHOW_GROUP,
					String.valueOf(a.isShowGroup()));
		}

		raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE,
				String.valueOf(!allowRemovalSheduling));
		if (allowRemovalSheduling)
		{
			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.REMOVAL_MODE,
					String.valueOf(removalMode));
		}
		raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ENABLE_PROJECT_MANAGEMENT_LINK,
				String.valueOf(enableUpMan));
		if (enableUpMan)
		{
			if (upManService != null && !upManService.isEmpty())
			{
				raw.put(HomeEndpointProperties.PREFIX
						+ HomeEndpointProperties.PROJECT_MANAGEMENT_ENDPOINT,
						upManService);
			}
		}

		raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.DISABLE_2ND_FACTOR_OPT_IN,
				String.valueOf(!allow2ndFactorOptIn));

		if (!enquiryForms.isEmpty())
		{
			enquiryForms.forEach(c -> raw.put(HomeEndpointProperties.PREFIX
					+ HomeEndpointProperties.ENQUIRIES + (enquiryForms.indexOf(c) + 1), c));
		}

		if(imageAttribute != null)
			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.AVATAR_IMAGE_ATTRIBUTE_NAME, imageAttribute);

		HomeEndpointProperties prop = new HomeEndpointProperties(raw);
		return prop.getAsString();
	}

	public void fromProperties(String properties, MessageSource msg, 
			List<Group> allGroups)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the home ui service", e);
		}

		HomeEndpointProperties homeProperties = new HomeEndpointProperties(raw);

		List<String> disabledComponents = homeProperties
				.getListOfValues(HomeEndpointProperties.DISABLED_COMPONENTS);

		enabledTabs = HomeServiceEditorComponent.getAvailableTabs();
		enabledUserDetailsControls = HomeServiceEditorComponent.getAvailableControls();

		enabledTabs.removeAll(disabledComponents);
		enabledUserDetailsControls.removeAll(disabledComponents);

		Set<String> attrKeys = homeProperties.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);
		for (String key : attrKeys)
		{
			ExposedAttribute attr = new ExposedAttribute();
			attr.setName(homeProperties.getValue(key + HomeEndpointProperties.GWA_ATTRIBUTE));

			String groupPath = homeProperties.getValue(key + HomeEndpointProperties.GWA_GROUP);
			attr.setGroup(new GroupWithIndentIndicator(
					allGroups.stream().filter(g -> g.toString().equals(groupPath))
							.findFirst().orElse(new Group(groupPath)),
					false));
			attr.setEditable(homeProperties
					.getBooleanValue(key + HomeEndpointProperties.GWA_EDITABLE));
			attr.setShowGroup(homeProperties
					.getBooleanValue(key + HomeEndpointProperties.GWA_SHOW_GROUP));
			exposedAttributes.add(attr);
		}

		removalMode = homeProperties.getEnumValue(HomeEndpointProperties.REMOVAL_MODE,
				HomeEndpointProperties.RemovalModes.class);
		allowRemovalSheduling = !homeProperties
				.getBooleanValue(HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE);
		allow2ndFactorOptIn = !homeProperties
				.getBooleanValue(HomeEndpointProperties.DISABLE_2ND_FACTOR_OPT_IN);

		enableUpMan = homeProperties
				.getBooleanValue(HomeEndpointProperties.ENABLE_PROJECT_MANAGEMENT_LINK);
		upManService = homeProperties.getValue(HomeEndpointProperties.PROJECT_MANAGEMENT_ENDPOINT);

		imageAttribute = homeProperties.getValue(HomeEndpointProperties.AVATAR_IMAGE_ATTRIBUTE_NAME);

		enquiryForms = homeProperties.getListOfValues(HomeEndpointProperties.ENQUIRIES);
	}

	public List<String> getEnabledTabs()
	{
		return enabledTabs;
	}

	public void setEnabledTabs(List<String> enabledTabs)
	{
		this.enabledTabs = enabledTabs;
	}

	public List<String> getEnabledUserDetailsControls()
	{
		return enabledUserDetailsControls;
	}

	public void setEnabledUserDetailsControls(List<String> enabledUserDetailsControls)
	{
		this.enabledUserDetailsControls = enabledUserDetailsControls;
	}

	public List<ExposedAttribute> getExposedAttributes()
	{
		return exposedAttributes;
	}

	public void setExposedAttributes(List<ExposedAttribute> exposedAttributes)
	{
		this.exposedAttributes = exposedAttributes;
	}

	public boolean isAllowRemovalSheduling()
	{
		return allowRemovalSheduling;
	}

	public void setAllowRemovalSheduling(boolean allowRemovalSheduling)
	{
		this.allowRemovalSheduling = allowRemovalSheduling;
	}

	public boolean isAllow2ndFactorOptIn()
	{
		return allow2ndFactorOptIn;
	}

	public void setAllow2ndFactorOptIn(boolean allow2ndFactorOptIn)
	{
		this.allow2ndFactorOptIn = allow2ndFactorOptIn;
	}

	public RemovalModes getRemovalMode()
	{
		return removalMode;
	}

	public void setRemovalMode(RemovalModes removalMode)
	{
		this.removalMode = removalMode;
	}

	public boolean isEnableUpMan()
	{
		return enableUpMan;
	}

	public void setEnableUpMan(boolean enableUpMan)
	{
		this.enableUpMan = enableUpMan;
	}

	public String getUpManService()
	{
		return upManService;
	}

	public void setUpManService(String upManService)
	{
		this.upManService = upManService;
	}

	public List<String> getEnquiryForms()
	{
		return enquiryForms;
	}

	public void setEnquiryForms(List<String> enquiryForms)
	{
		this.enquiryForms = enquiryForms;
	}

	public String getImageAttribute()
	{
		return imageAttribute;
	}

	public void setImageAttribute(String imageAttribute)
	{
		this.imageAttribute = imageAttribute;
	}
}