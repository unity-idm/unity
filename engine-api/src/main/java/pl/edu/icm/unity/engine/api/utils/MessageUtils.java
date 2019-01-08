/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.util.Collection;
import java.util.Iterator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Messages related utilities
 * @author K. Benedyczak
 */
public class MessageUtils
{
	/**
	 * Produces a string with at most 4 elements of the collection elements (toString() is called).
	 * If there are more elements an indication is appended.
	 * @param msg
	 * @param objects
	 * @return
	 */
	public static String createConfirmFromStrings(UnityMessageSource msg, Collection<?> objects)
	{
		StringBuilder confirmText = new StringBuilder();
		Iterator<?> it = objects.iterator();
		final int MAX = 4;
		for (int i=0; i<MAX && it.hasNext(); i++)
			confirmText.append(", ").append(it.next());
		if (it.hasNext())
			confirmText.append(msg.getMessage("MessageUtils.andMore", objects.size() - MAX));
		return confirmText.substring(2);
	}
	
	/**
	 * As {@link #createConfirmFromStrings(UnityMessageSource, Collection)} but uses getName() instead of toString().
	 * @param msg
	 * @param objects
	 * @return
	 */
	public static String createConfirmFromNames(UnityMessageSource msg, 
			Collection<? extends NamedObject> objects)
	{
		StringBuilder confirmText = new StringBuilder();
		Iterator<? extends NamedObject> it = objects.iterator();
		final int MAX = 4;
		for (int i=0; i<MAX && it.hasNext(); i++)
			confirmText.append(", ").append(it.next().getName());
		if (it.hasNext())
			confirmText.append(msg.getMessage("MessageUtils.andMore", objects.size() - MAX));
		return confirmText.substring(2);
	}
}
