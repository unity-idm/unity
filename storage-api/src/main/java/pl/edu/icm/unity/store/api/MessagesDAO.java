/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.base.message.Message;

/**
 * Messages DAO
 * 
 * @author P.Piernik
 *
 */
public interface MessagesDAO extends BasicCRUDDAO<Message>
{
	String DAO_ID = "MessageDAO";
	String NAME = "Message";

	List<Message> getByName(String key);
	Message getByNameAndLocale(String key, String locale);
	void update(Message msg);
	void deleteByNameAndLocale(String key, String locale);
	void deleteByName(String key);

}
