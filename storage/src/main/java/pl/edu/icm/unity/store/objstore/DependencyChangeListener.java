/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

/**
 * Implementation is notified about the pre- add/update/remove operations on a 
 * generic object. The type of the dependency generic object is returned by the listener.
 * <p>
 * The implementation may either perform maintenance of its object or block the whole operation by
 * throwing an exception.
 * 
 * 
 * @author K. Benedyczak
 */
public interface DependencyChangeListener<T>
{
	public String getDependencyObjectType();
	public void preAdd(T newObject);
	public void preUpdate(T oldObject, T updatedObject);
	public void preRemove(T removedObject);
}
