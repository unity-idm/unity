/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
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
}