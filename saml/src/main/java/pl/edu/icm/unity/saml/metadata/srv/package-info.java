/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 *
 * Created on 29.09.2017
 * Author: K. Benedyczak
 */

/**
 * Global, shared metadata downloading service. 
 * Consumers can register with their metadata to be downloaded.
 * Service ensures that the the same metadata is downloaded exactly once.
 * The service is notifying the consumer when a new metadata is available.
 * @author K. Benedyczak
 */
package pl.edu.icm.unity.saml.metadata.srv;