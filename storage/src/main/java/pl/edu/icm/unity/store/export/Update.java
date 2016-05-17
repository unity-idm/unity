/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation should perform update of JSON data between subsequent versions.
 * @author K. Benedyczak
 */
public interface Update
{
	void update(InputStream is) throws IOException;
}
