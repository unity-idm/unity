/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.server;

import java.net.URL;

public interface AdvertisedAddressProvider
{
	/**
	 * @return base address of the server which should be used as its
	 *         externally accessible address.
	 */
	URL get();
}
