/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.io.File;
import java.io.IOException;

/**
 * Provides feature to export and import database dumps to a file. 
 * <p>
 * The implementation must allows for updating imported contents if it was stored by an older Unity version. 
 * @author K. Benedyczak
 */
public interface ImportExport
{
	void loadFromFile(File file) throws IOException;
	void storeToFile(File file) throws IOException;
}
