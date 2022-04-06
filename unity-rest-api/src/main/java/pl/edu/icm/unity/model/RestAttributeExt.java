/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class RestAttributeExt
{
	private boolean direct;
	private Date creationTs;
	private Date updateTs;
	private String name;
	private String valueSyntax;
	private String groupPath;
	private List<String> values;
	private String translationProfile;
	private String remoteIdp;

	public RestAttributeExt(boolean direct, Date creationTs, Date updateTs, String name, String valueSyntax,
	                        String groupPath, List<String> values, String translationProfile, String remoteIdp)
	{
		this.direct = direct;
		this.creationTs = creationTs;
		this.updateTs = updateTs;
		this.name = name;
		this.valueSyntax = valueSyntax;
		this.groupPath = groupPath;
		this.values = values;
		this.translationProfile = translationProfile;
		this.remoteIdp = remoteIdp;
	}

	//for Jackson
	protected RestAttributeExt()
	{
	}

	public boolean isDirect()
	{
		return direct;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}

	public Date getUpdateTs()
	{
		return updateTs;
	}

	public String getName()
	{
		return name;
	}

	public String getValueSyntax()
	{
		return valueSyntax;
	}

	public String getGroupPath()
	{
		return groupPath;
	}

	public List<String> getValues()
	{
		return values;
	}

	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public String getRemoteIdp()
	{
		return remoteIdp;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestAttributeExt that = (RestAttributeExt) o;
		return direct == that.direct && Objects.equals(creationTs, that.creationTs) && Objects.equals(updateTs, that.updateTs) && Objects.equals(name, that.name) && Objects.equals(valueSyntax, that.valueSyntax) && Objects.equals(groupPath, that.groupPath) && Objects.equals(values, that.values) && Objects.equals(translationProfile, that.translationProfile) && Objects.equals(remoteIdp, that.remoteIdp);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(direct, creationTs, updateTs, name, valueSyntax, groupPath, values, translationProfile, remoteIdp);
	}

	@Override
	public String toString()
	{
		return "RestAttributeExt{" +
				"direct=" + direct +
				", creationTs=" + creationTs +
				", updateTs=" + updateTs +
				", name='" + name + '\'' +
				", valueSyntax='" + valueSyntax + '\'' +
				", groupPath='" + groupPath + '\'' +
				", values=" + values +
				", translationProfile='" + translationProfile + '\'' +
				", remoteIdp='" + remoteIdp + '\'' +
				'}';
	}
}
