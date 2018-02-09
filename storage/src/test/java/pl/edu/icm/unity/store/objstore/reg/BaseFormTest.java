/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.NamedObject;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.registration.BaseForm;

public abstract class BaseFormTest<T extends BaseForm> extends AbstractNamedWithTSTest<T>
{
	@Autowired
	private CredentialDB credentialDB;
	
	@Autowired
	private GroupDAO groupDB;
	
	@Autowired
	private AttributeTypeDAO atDB;
	
	@Autowired
	private MessageTemplateDB msgTplDB;
	
	@Test
	public void usedCredRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "cred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setConfiguration("");
			credentialDB.create(cred);
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(credentialDB).delete("cred");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void usedCredRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new CredentialDefinition("typeId", "cred", 
						new I18nString("dName"), new I18nString("desc")), 
				new CredentialDefinition("typeId", "cred2", 
						new I18nString("dName"), new I18nString("desc")), 
				credentialDB);
		
		assertThat(afterDependencyRename.getCredentialParams().get(0).getCredentialName(), 
					is("cred2"));
	}
	
	protected <X extends NamedObject> T renameTest(X dep, X depRenamed, NamedCRUDDAO<X> dao)
	{
		return tx.runInTransactionRet(() -> {
			dao.create(dep);
			T obj = getObject("name1");
			getDAO().create(obj);

			dao.updateByName(dep.getName(), depRenamed);
			
			return getDAO().get(obj.getName());
		});
	}
	
	@Test
	public void usedGroupInAttrRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			groupDB.create(new Group("/C"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(groupDB).delete("/C");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void usedGroupInAttrRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new Group("/C"), 
				new Group("/ZZZ"), 
				groupDB);
		
		assertThat(afterDependencyRename.getAttributeParams().get(0).getGroup(), 
					is("/ZZZ"));
	}

	
	@Test
	public void usedGroupInNotificationRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			groupDB.create(new Group("/notifyGrp"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(groupDB).delete("/notifyGrp");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void usedGroupInNotificationRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new Group("/notifyGrp"), 
				new Group("/ZZZ"), 
				groupDB);
		
		assertThat(afterDependencyRename.getNotificationsConfiguration().getAdminsNotificationGroup(), 
					is("/ZZZ"));
	}

	
	@Test
	public void usedGroupInGroupsRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			groupDB.create(new Group("/B"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(groupDB).delete("/B");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void usedGroupInGroupsRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new Group("/B"), 
				new Group("/ZZZ"), 
				groupDB);
		
		assertThat(afterDependencyRename.getGroupParams().get(0).getGroupPath(), 
					is("/ZZZ"));
	}
	
	@Test
	public void parentOfUsedGroupInGroupsRenameIsPropagated()
	{
		tx.runInTransaction(() -> {
			groupDB.create(new Group("/C"));
			groupDB.create(new Group("/C/sub"));
			T obj = getObject("name1");
			obj.getGroupParams().get(0).setGroupPath("/C/sub");
			getDAO().create(obj);

			groupDB.updateByName("/C", new Group("/ZZZ"));
			
			T afterDependencyRename = getDAO().get(obj.getName());
			
			assertThat(afterDependencyRename.getGroupParams().get(0).getGroupPath(), 
					is("/ZZZ/sub"));
		});
	}
	
	@Test
	public void usedAttrTypeRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			atDB.create(new AttributeType("email", "foo"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(atDB).delete("email");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void usedAttrTypeRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new AttributeType("email", "foo"), 
				new AttributeType("changed", "foo"), 
				atDB);
		
		assertThat(afterDependencyRename.getAttributeParams().get(0).getAttributeType(),
					is("changed"));
	}

	
	@Test
	public void usedMsgTmplRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			msgTplDB.create(new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
					new I18nString("")), "consumer", MessageType.PLAIN, "channel"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(msgTplDB).delete("template");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});		
	}

	@Test
	public void usedMsgTmplRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
						new I18nString("")), AcceptRegistrationTemplateDef.NAME, MessageType.PLAIN, "channel"), 
				new MessageTemplate("changed", "", new I18nMessage(new I18nString(""),
						new I18nString("")), AcceptRegistrationTemplateDef.NAME, MessageType.PLAIN, "channel"), 
				msgTplDB);
		
		assertThat(afterDependencyRename.getNotificationsConfiguration().getAcceptedTemplate(),
					is("changed"));
	}
	
	@Test
	public void usedMsgTmplUpdateToIncompatibleIsRestricted()
	{
		tx.runInTransaction(() -> {
			msgTplDB.create(new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
					new I18nString("")), SubmitRegistrationTemplateDef.NAME, MessageType.PLAIN, "channel"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(msgTplDB).update(new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
					new I18nString("")), "CHANGED-CONSUMER", MessageType.PLAIN, "channel"));
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
}
