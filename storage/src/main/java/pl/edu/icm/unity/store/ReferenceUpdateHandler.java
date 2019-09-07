/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.EnumSet;

import pl.edu.icm.unity.store.types.UpdateFlag;

/**
 * Implementations of this interface are called before update of a referenced element.
 * @author K. Benedyczak
 */
public interface ReferenceUpdateHandler<T>
{
	void preUpdateCheck(PlannedUpdateEvent<T> update);
	
	public class PlannedUpdateEvent<T>
	{
		public final long modifiedId;
		/**
		 * Original name or null if the element has no name
		 */
		public final String modifiedName;
		public final T newValue;
		public final T oldValue;
		public final EnumSet<UpdateFlag> updateFlags;
		
		public PlannedUpdateEvent(long modifiedId, String modifiedName, T newValue, T oldValue,
				EnumSet<UpdateFlag> updateFlags)
		{
			this.modifiedId = modifiedId;
			this.modifiedName = modifiedName;
			this.newValue = newValue;
			this.oldValue = oldValue;
			this.updateFlags = updateFlags;
		}
		
		public PlannedUpdateEvent(long modifiedId, String modifiedName, T newValue, T oldValue)
		{
			this(modifiedId, modifiedName, newValue, oldValue, EnumSet.noneOf(UpdateFlag.class));
		}
	}
}
