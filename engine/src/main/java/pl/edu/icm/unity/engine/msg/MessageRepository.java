/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.msg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msg.Message;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
public class MessageRepository
{
	private MessagesDAO dao;
	private List<Message> all;

	@Autowired
	public MessageRepository(MessagesDAO dao, TransactionalRunner tx) throws EngineException
	{
		this.dao = dao;	
		all = new ArrayList<>();
	}

	@Transactional
	public void reload()
	{
		all = dao.getAll();
	}

	public Optional<String> get(String name, Locale locale)
	{
		Optional<Message> message = all.stream()
				.filter(m -> m.getName().equals(name) && m.getLocale().equals(locale))
				.findFirst();
		return message.isPresent() ? Optional.of(message.get().getValue()) : Optional.empty();
	}
}
