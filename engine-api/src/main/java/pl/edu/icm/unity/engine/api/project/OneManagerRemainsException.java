package pl.edu.icm.unity.engine.api.project;

import pl.edu.icm.unity.base.exceptions.InternalException;

public class OneManagerRemainsException extends InternalException
{
	public OneManagerRemainsException(String group)
	{
		super("At least one manager must remain in group " + group);
	}
}
