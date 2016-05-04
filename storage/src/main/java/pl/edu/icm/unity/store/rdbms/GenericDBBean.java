/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;



/**
 * In DB representation of basic data which is present in the most of tables.
 * @author K. Benedyczak
 */
public interface GenericDBBean
{
	Long getId();
	void setId(Long id);
	byte[] getContents();
	void setContents(byte[] contents);
}
