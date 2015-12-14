/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

/**
 * Information about a group membership to be added for an entity being registered.
 * Can 
 * @author K. Benedyczak
 */
public class GroupParam
{
	private String group;
	private String externalIdp;
	private String translationProfile;

	public GroupParam(String group, String externalIdp, String translationProfile)
	{
		this.group = group;
		this.externalIdp = externalIdp;
		this.translationProfile = translationProfile;
	}
	public String getGroup()
	{
		return group;
	}
	public String getExternalIdp()
	{
		return externalIdp;
	}
	public String getTranslationProfile()
	{
		return translationProfile;
	}
}
