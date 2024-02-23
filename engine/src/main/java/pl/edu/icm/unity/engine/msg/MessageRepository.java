/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.msg;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.Message;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
public class MessageRepository
{
	private MessagesDAO dao;
	private Map<String, Map<Locale, String>> all;

	@Autowired
	public MessageRepository(MessagesDAO dao, TransactionalRunner tx) throws EngineException
	{
		this.dao = dao;	
		all = new HashMap<>();
	}

	@Transactional
	public void reload()
	{
		all.clear();
		for (Message m : dao.getAll())
		{
			if (all.containsKey(m.getName()))
			{
				all.get(m.getName()).put(m.getLocale(), m.getValue());
			}else
			{
				
				Map<Locale, String> v = new HashMap<>();
				v.put(m.getLocale(), m.getValue());
				all.put(m.getName(), v);
			}
		}
	}

	public Optional<String> get(String name, Locale locale)
	{
		String message = null;
		if (all.containsKey(name))
		{
			message = all.get(name).get(locale);
		}	
		return message != null ? Optional.of(message) : Optional.empty();
	}
}
