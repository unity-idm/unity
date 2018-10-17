/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Information about selected group or groups, corresponding to a single form choice.
 * 
 * @author K. Benedyczak
 */
public class GroupSelection
{
	private List<String> selectedGroups = new ArrayList<>();
	private String externalIdp;
	private String translationProfile;

	public GroupSelection()
	{
	}

	public GroupSelection(String group)
	{
		selectedGroups.add(group);
	}
	
	public GroupSelection(List<String> groups)
	{
		selectedGroups.addAll(groups);
	}
	
	public GroupSelection(String group, String externalIdp, String translationProfile)
	{
		selectedGroups.add(group);
		this.externalIdp = externalIdp;
		this.translationProfile = translationProfile;
	}
	
	public GroupSelection(List<String> groups, String externalIdp, String translationProfile)
	{
		selectedGroups.addAll(groups);
		this.externalIdp = externalIdp;
		this.translationProfile = translationProfile;
	}
	
	public List<String> getSelectedGroups()
	{
		return new ArrayList<>(selectedGroups);
	}
	public void setSelectedGroups(List<String> selectedGroups)
	{
		this.selectedGroups = new ArrayList<>(selectedGroups);
	}
	public String getExternalIdp()
	{
		return externalIdp;
	}
	public void setExternalIdp(String externalIdp)
	{
		this.externalIdp = externalIdp;
	}
	public String getTranslationProfile()
	{
		return translationProfile;
	}
	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof GroupSelection))
			return false;
		GroupSelection castOther = (GroupSelection) other;
		return Objects.equals(selectedGroups, castOther.selectedGroups)
				&& Objects.equals(externalIdp, castOther.externalIdp)
				&& Objects.equals(translationProfile, castOther.translationProfile);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(selectedGroups, externalIdp, translationProfile);
	}
}
