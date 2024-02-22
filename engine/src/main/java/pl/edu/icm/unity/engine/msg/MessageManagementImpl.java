/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.msg;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.Message;
import pl.edu.icm.unity.engine.api.msg.MessageManagement;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.base.tx.Transactional;

/**
 * Implementation of {@link MessageManagement}
 * 
 * @author P.Piernik
 *
 */
@Component
public class MessageManagementImpl implements MessageManagement
{
	private MessagesDAO dao;
	private MessageRepository repository;
	private InternalAuthorizationManager authz;

	public MessageManagementImpl(InternalAuthorizationManager authz, MessagesDAO dao, MessageRepository repository)
	{
		this.authz = authz;
		this.dao = dao;
		this.repository = repository;
	}

	@Override
	@Transactional
	public void addMessage(Message message) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.create(message);
		repository.reload();

	}

	@Override
	@Transactional
	public void removeMessage(String name, Locale locale) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.deleteByNameAndLocale(name, locale.toString());
		repository.reload();
	}

	@Override
	@Transactional
	public List<Message> getAllMessages() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return dao.getAll();
	}

	@Override
	@Transactional
	public void removeMessage(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.deleteByName(name);
		repository.reload();
	}

	@Override
	@Transactional
	public List<Message> getMessages(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return dao.getByName(name);
	}

	@Override
	@Transactional
	public void updateMessage(Message message) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.update(message);
		repository.reload();
	}
}
