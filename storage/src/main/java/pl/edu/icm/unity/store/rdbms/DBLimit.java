/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import pl.edu.icm.unity.store.rdbms.model.DBLimitsBean;

public class DBLimit
{
	private DBLimitsBean bean;
	
	public DBLimit(DBLimitsBean bean)
	{
		this.bean = bean;
	}

	public void checkNameLimit(String ofWhat) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length() > bean.getNameLimit())
			throw new IllegalArgumentException("Name length must not exceed " + 
					bean.getNameLimit() + " characters");

	}
	
	public void checkContentsLimit(byte[] ofWhat) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length > bean.getContentsLimit())
			throw new IllegalArgumentException("Contents must not exceed " + 
					bean.getContentsLimit() + " bytes");

	} 
}
