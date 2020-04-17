/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.messages;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Access to Messages.xml operations
 *
 * @author P.Piernik
 *
 */
public interface MessagesMapper extends BasicCRUDMapper<MessageBean>
{
	List<MessageBean> getByName(String name);
	MessageBean getByNameAndLocale(MessageBean bean);
	void deleteByNameAndLocale(MessageBean bean);
	void deleteByName(String name);
}
