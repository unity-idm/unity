/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

/**
 * Implementations allow for registering {@link ReferenceRemovalHandler} and {@link ReferenceUpdateHandler}
 * to be notified about object changes in the implementing DAO.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
public interface ReferenceAwareDAO<T>
{
	void addRemovalHandler(ReferenceRemovalHandler handler);
	void addUpdateHandler(ReferenceUpdateHandler<T> handler);
}
