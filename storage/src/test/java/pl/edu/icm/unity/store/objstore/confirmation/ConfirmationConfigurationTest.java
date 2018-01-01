/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.confirmation;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.msgtemplates.confirm.ConfirmationTemplateDef;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.generic.ConfirmationConfigurationDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;

public class ConfirmationConfigurationTest extends AbstractNamedWithTSTest<ConfirmationConfiguration>
{
	@Autowired
	private ConfirmationConfigurationDB dao;
	
	@Autowired
	private AttributeTypeDAO attributeTypeDAO;

	@Autowired
	private MessageTemplateDB msgTmplDAO;
	
	@Override
	protected NamedCRUDDAOWithTS<ConfirmationConfiguration> getDAO()
	{
		return dao;
	}

	@Test
	public void attributeTypeRemovalRemovesConfirmationConfig()
	{
		tx.runInTransaction(() -> {
			AttributeType created = new AttributeType("attr", "syntaxId");
			attributeTypeDAO.create(created);
			
			ConfirmationConfiguration obj = getObject("attr");
			dao.create(obj);

			attributeTypeDAO.delete("attr");
			
			assertThat(dao.exists(obj.getName()), is(false));
		});
	}

	@Test
	public void attributeTypeRenameIsPropagated()
	{
		tx.runInTransaction(() -> {
			AttributeType created = new AttributeType("attr", "syntaxId");
			long atKey = attributeTypeDAO.create(created);
			
			ConfirmationConfiguration obj = getObject("attr");
			dao.create(obj);

			AttributeType renamed = new AttributeType("attr2", "syntaxId");
			attributeTypeDAO.updateByKey(atKey, renamed);
			
			assertThat(dao.exists(obj.getName()), is(false));
			obj.setNameToConfirm("attr2");
			assertThat(dao.exists(obj.getName()), is(true));
		});
	}
	
	@Test
	public void templateRemovalIsBlocked()
	{
		tx.runInTransaction(() -> {
			MessageTemplate msgT = new MessageTemplate("msgTemplate", "description",
					new I18nMessage(new I18nString("s"), new I18nString("b")),
					"consumer", MessageType.PLAIN);
			msgTmplDAO.create(msgT);
			
			ConfirmationConfiguration obj = getObject("name1");
			dao.create(obj);

			catchException(msgTmplDAO).delete("msgTemplate");
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void changeOfTemplateConsumerIsBlocked()
	{
		tx.runInTransaction(() -> {
			MessageTemplate msgT = new MessageTemplate("msgTemplate", "description",
					new I18nMessage(new I18nString("s"), new I18nString("b")),
					ConfirmationTemplateDef.NAME, MessageType.PLAIN);
			msgTmplDAO.create(msgT);
			
			ConfirmationConfiguration obj = getObject("name1");
			dao.create(obj);

			MessageTemplate msgT2 = new MessageTemplate("msgTemplate", "description",
					new I18nMessage(new I18nString("s"), new I18nString("b")),
					"consumer2", MessageType.PLAIN);
			catchException(msgTmplDAO).updateByName("msgTemplate", msgT2);
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}	
	
	@Override
	protected ConfirmationConfiguration getObject(String id)
	{
		return ConfirmationConfiguration.builder()
				.withMsgTemplate("msgTemplate")
				.withNameToConfirm(id)
				.withNotificationChannel("notificationChannel")
				.withTypeToConfirm(ConfirmationConfigurationDB.ATTRIBUTE_CONFIG_TYPE)
				.build();
	}

	@Override
	protected ConfirmationConfiguration mutateObject(ConfirmationConfiguration cc)
	{
		cc.setMsgTemplate("msgTemplate2");
		cc.setNameToConfirm("attr2");
		cc.setNotificationChannel("notificationChannel2");
		cc.setTypeToConfirm(ConfirmationConfigurationDB.IDENTITY_CONFIG_TYPE);
		return cc;
	}
}
