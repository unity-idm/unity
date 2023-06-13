/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.messages;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.message.Message;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * RDBMS storage of {@link Message}
 * 
 * @author P.Piernik
 */
@Repository(MessageRDBMSStore.BEAN)
class MessageRDBMSStore extends GenericRDBMSCRUD<Message, MessageBean> implements MessagesDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	
	@Autowired
	public MessageRDBMSStore(MessageJsonSerializer serializer)
	{
		super(MessagesMapper.class, serializer, NAME);
	}

	@Override
	public List<Message> getByName(String key)
	{
		MessagesMapper mapper = SQLTransactionTL.getSql().getMapper(MessagesMapper.class);
		List<MessageBean> fromDB = mapper.getByName(key);
		return convertList(fromDB);
	}

	@Override
	public Message getByNameAndLocale(String key, String locale)
	{
		MessagesMapper mapper = SQLTransactionTL.getSql().getMapper(MessagesMapper.class);
		MessageBean fromDB = mapper.getByNameAndLocale(new MessageBean(key, locale));
		return jsonSerializer.fromDB(fromDB);
	}

	@Override
	public void update(Message msg)
	{
		MessagesMapper mapper = SQLTransactionTL.getSql().getMapper(MessagesMapper.class);
		MessageBean message = mapper.getByNameAndLocale(jsonSerializer.toDB(msg));
		if (message == null)
			throw new IllegalArgumentException(elementName + " [" + msg.getName() + 
					"] does not exist");
		message.setContents(msg.getValue().getBytes());
		mapper.updateByKey(message);		
	}

	@Override
	public void deleteByNameAndLocale(String key, String locale)
	{
		MessagesMapper mapper = SQLTransactionTL.getSql().getMapper(MessagesMapper.class);
		mapper.deleteByNameAndLocale(new MessageBean(key, locale));
	}

	@Override
	public void deleteByName(String key)
	{
		MessagesMapper mapper = SQLTransactionTL.getSql().getMapper(MessagesMapper.class);
		mapper.deleteByName(key);
		
	}
	
}
