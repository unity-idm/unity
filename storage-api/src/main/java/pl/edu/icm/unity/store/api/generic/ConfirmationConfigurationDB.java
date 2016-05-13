/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.confirmations.ConfirmationConfiguration;

/**
 * Easy access to {@link ConfirmationConfiguration} storage.
 * 
 * @author K. Benedyczak
 */
public interface ConfirmationConfigurationDB extends GenericObjectsDAO<ConfirmationConfiguration>
{
	public final String ATTRIBUTE_CONFIG_TYPE = "attribute";
	public final String IDENTITY_CONFIG_TYPE = "identity";
}
