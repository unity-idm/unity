/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration.invitation;

import java.util.Objects;

import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.Selection;

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
		return Objects.hash(entry, mode);
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
		return Objects.equals(entry, other.entry) && mode == other.mode;
	}
}