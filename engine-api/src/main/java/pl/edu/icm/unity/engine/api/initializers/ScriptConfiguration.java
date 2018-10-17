/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.initializers;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Holds parsed scripts configuration {@link UnityServerConfiguration#SCRIPTS}.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ScriptConfiguration
{
	private ScriptType type;
	private String trigger;
	private String location;
	
	
	public ScriptConfiguration(ScriptType type, String trigger, String location)
	{
		this.type = type;
		this.trigger = trigger;
		this.location = location;
	}

	public String getTrigger()
	{
		return trigger;
	}

	public ScriptType getType()
	{
		return type;
	}

	public void setType(ScriptType type)
	{
		this.type = type;
	}

	public String getFileLocation()
	{
		return location;
	}

	@Override
	public String toString()
	{
		return "ContentInitConf [type=" + type + ", trigger=" + trigger + ", location="
				+ location + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((trigger == null) ? 0 : trigger.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		ScriptConfiguration other = (ScriptConfiguration) obj;
		if (location == null)
		{
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (trigger == null)
		{
			if (other.trigger != null)
				return false;
		} else if (!trigger.equals(other.trigger))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
