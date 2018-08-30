/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((externalIdp == null) ? 0 : externalIdp.hashCode());
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((translationProfile == null) ? 0 : translationProfile.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroupParam other = (GroupParam) obj;
		if (externalIdp == null)
		{
			if (other.externalIdp != null)
				return false;
		} else if (!externalIdp.equals(other.externalIdp))
			return false;
		if (group == null)
		{
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (translationProfile == null)
		{
			if (other.translationProfile != null)
				return false;
		} else if (!translationProfile.equals(other.translationProfile))
			return false;
		return true;
	}
}
