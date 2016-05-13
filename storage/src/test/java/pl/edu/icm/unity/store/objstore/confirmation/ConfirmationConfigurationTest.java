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

import pl.edu.icm.unity.base.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.base.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.generic.ConfirmationConfigurationDB;
import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.mocks.MockAttributeSyntax;
import pl.edu.icm.unity.store.mocks.MockVerifiableAttributeSyntax;
import pl.edu.icm.unity.store.objstore.AbstractObjStoreTest;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class ConfirmationConfigurationTest extends AbstractObjStoreTest<ConfirmationConfiguration>
{
	@Autowired
	private ConfirmationConfigurationDB dao;
	
	@Autowired
	private AttributeTypeDAO attributeTypeDAO;

	@Autowired
	private MessageTemplateDB msgTmplDAO;
	
	@Override
	protected GenericObjectsDAO<ConfirmationConfiguration> getDAO()
	{
		return dao;
	}

	@Test
	public void attributeTypeRemovalRemovesConfirmationConfig()
	{
		tx.runInTransaction(() -> {
			AttributeType created = new AttributeType("attr", new MockVerifiableAttributeSyntax());
			attributeTypeDAO.create(created);
			
			ConfirmationConfiguration obj = getObject("attr");
			dao.insert(obj);

			attributeTypeDAO.delete("attr");
			
			assertThat(dao.exists(obj.getName()), is(false));
		});
	}

	@Test
	public void attributeTypeRenameIsPropagated()
	{
		tx.runInTransaction(() -> {
			AttributeType created = new AttributeType("attr", new MockVerifiableAttributeSyntax());
			long atKey = attributeTypeDAO.create(created);
			
			ConfirmationConfiguration obj = getObject("attr");
			dao.insert(obj);

			AttributeType renamed = new AttributeType("attr2", new MockVerifiableAttributeSyntax());
			attributeTypeDAO.updateByKey(atKey, renamed);
			
			assertThat(dao.exists(obj.getName()), is(false));
			obj.setNameToConfirm("attr2");
			assertThat(dao.exists(obj.getName()), is(true));
		});
	}
	
	@Test
	public void attributeTypeChangeToNonVerifiableRemovesConfirmationConfig()
	{
		tx.runInTransaction(() -> {
			AttributeType created = new AttributeType("attr", new MockVerifiableAttributeSyntax());
			attributeTypeDAO.create(created);
			
			ConfirmationConfiguration obj = getObject("attr");
			dao.insert(obj);
			
			AttributeType changed = new AttributeType("attr", new MockAttributeSyntax());
			attributeTypeDAO.update(changed);
			
			assertThat(dao.exists(obj.getName()), is(false));
		});
	}
	
	@Test
	public void templateRemovalIsBlocked()
	{
		tx.runInTransaction(() -> {
			MessageTemplate msgT = new MessageTemplate("msgTemplate", "description",
					new I18nMessage(new I18nString("s"), new I18nString("b")),
					"consumer");
			msgTmplDAO.insert(msgT);
			
			ConfirmationConfiguration obj = getObject("name1");
			dao.insert(obj);

			catchException(msgTmplDAO).remove("msgTemplate");
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void changeOfTemplateConsumerIsBlocked()
	{
		tx.runInTransaction(() -> {
			MessageTemplate msgT = new MessageTemplate("msgTemplate", "description",
					new I18nMessage(new I18nString("s"), new I18nString("b")),
					ConfirmationTemplateDef.NAME);
			msgTmplDAO.insert(msgT);
			
			ConfirmationConfiguration obj = getObject("name1");
			dao.insert(obj);

			MessageTemplate msgT2 = new MessageTemplate("msgTemplate", "description",
					new I18nMessage(new I18nString("s"), new I18nString("b")),
					"consumer2");
			catchException(msgTmplDAO).update("msgTemplate", msgT2);
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}	
	
	@Override
	protected ConfirmationConfiguration getObject(String id)
	{
		ConfirmationConfiguration cc = new ConfirmationConfiguration();
		cc.setMsgTemplate("msgTemplate");
		cc.setNameToConfirm(id);
		cc.setNotificationChannel("notificationChannel");
		cc.setTypeToConfirm(ConfirmationConfigurationDB.ATTRIBUTE_CONFIG_TYPE);
		return cc;
	}

	@Override
	protected void mutateObject(ConfirmationConfiguration cc)
	{
		cc.setMsgTemplate("msgTemplate2");
		cc.setNameToConfirm("attr2");
		cc.setNotificationChannel("notificationChannel2");
		cc.setTypeToConfirm(ConfirmationConfigurationDB.IDENTITY_CONFIG_TYPE);
	}

	@Override
	protected void assertAreEqual(ConfirmationConfiguration obj, ConfirmationConfiguration cmp)
	{
		assertThat(obj, is(cmp));
	}
}
