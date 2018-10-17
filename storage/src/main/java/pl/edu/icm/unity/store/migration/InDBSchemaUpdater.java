/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import java.io.IOException;

/**
 * Implementation should update (one step) DB in place
 * @author K. Benedyczak
 */
public interface InDBSchemaUpdater
{
	void update() throws IOException;
}
