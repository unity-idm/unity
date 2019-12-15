/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import java.io.IOException;

/**
 * Implementation should update DB contents in place, for one DB version increment.
 * This is responsible for data migration, SQL schema is handled before those updaters are invoked.
 * @author K. Benedyczak
 */
public interface InDBContentsUpdater
{
	void update() throws IOException;
	
	int getUpdatedVersion();
}
