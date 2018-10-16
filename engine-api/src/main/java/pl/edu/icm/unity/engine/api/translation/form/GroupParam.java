/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

import java.util.Objects;

/**
 * Information about a group membership to be added for an entity being registered.
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
	public String toString()
	{
		return group;
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof GroupParam))
			return false;
		GroupParam castOther = (GroupParam) other;
		return Objects.equals(group, castOther.group) && Objects.equals(externalIdp, castOther.externalIdp)
				&& Objects.equals(translationProfile, castOther.translationProfile);
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(group, externalIdp, translationProfile);
	}
}
