/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.translation;

/**
 * Describes a translation action implementation.
 * 
 * @author Krzysztof Benedyczak
 */
public class TranslationActionType
{
	private ProfileType supportedProfileType;
	private String descriptionKey;
	private String name;
	private ActionParameterDefinition[] parameters;

	public TranslationActionType(ProfileType supportedProfileType, String descriptionKey,
			String name, ActionParameterDefinition[] parameters)
	{
		super();
		this.supportedProfileType = supportedProfileType;
		this.descriptionKey = descriptionKey;
		this.name = name;
		this.parameters = parameters;
	}

	public ProfileType getSupportedProfileType()
	{
		return supportedProfileType;
	}
	public String getDescriptionKey()
	{
		return descriptionKey;
	}
	public String getName()
	{
		return name;
	}
	public ActionParameterDefinition[] getParameters()
	{
		return parameters;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime
				* result
				+ ((supportedProfileType == null) ? 0 : supportedProfileType
						.hashCode());
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
		TranslationActionType other = (TranslationActionType) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (supportedProfileType != other.supportedProfileType)
			return false;
		return true;
	}
}
