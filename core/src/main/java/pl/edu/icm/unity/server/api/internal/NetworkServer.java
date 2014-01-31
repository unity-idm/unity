/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.net.URL;

/**
 * Provides access to the information of the network server.
 * @author K. Benedyczak
 */
public interface NetworkServer
{
	/**
	 * @return base address of the server which should be used as its externally accessible address.
	 */
	public URL getAdvertisedAddress();
}
