/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.Selection;

/**
 * Enhanced information about prefilled entry
 *
 * @author Krzysztof Benedyczak
 * @param <T> type of the entry as {@link IdentityParam} or group {@link Selection}
 */
public class PrefilledEntry<T>
{
	private T entry;
	private PrefilledEntryMode mode;

	public PrefilledEntry(T entry, PrefilledEntryMode mode)
	{
		super();
		this.entry = entry;
		this.mode = mode;
	}
	public T getEntry()
	{
		return entry;
	}
	public PrefilledEntryMode getMode()
	{
		return mode;
	}
	
	@Override
	public String toString()
	{
		return "[" + mode.name() + "] " + entry.toString(); 
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entry == null) ? 0 : entry.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
		PrefilledEntry<?> other = (PrefilledEntry<?>) obj;
		if (entry == null)
		{
			if (other.entry != null)
				return false;
		} else if (!entry.equals(other.entry))
			return false;
		if (mode != other.mode)
			return false;
		return true;
	}
}