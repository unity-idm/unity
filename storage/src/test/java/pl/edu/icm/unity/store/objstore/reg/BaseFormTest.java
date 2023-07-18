/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.base.msg_template.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

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

			Throwable error = catchThrowable(() -> credentialDB.delete("cred"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
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
		
		assertThat(afterDependencyRename.getCredentialParams().get(0).getCredentialName()).isEqualTo("cred2");
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

			Throwable error = catchThrowable(() -> groupDB.delete("/C"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void usedGroupInAttrRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new Group("/C"), 
				new Group("/ZZZ"), 
				groupDB);
		
		assertThat(afterDependencyRename.getAttributeParams().get(0).getGroup()).isEqualTo("/ZZZ");
	}

	
	@Test
	public void usedGroupInNotificationRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			groupDB.create(new Group("/notifyGrp"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> groupDB.delete("/notifyGrp"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void usedGroupInNotificationRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new Group("/notifyGrp"), 
				new Group("/ZZZ"), 
				groupDB);
		
		assertThat(afterDependencyRename.getNotificationsConfiguration().getAdminsNotificationGroup()).isEqualTo("/ZZZ");
	}

	
	@Test
	public void usedGroupInGroupsRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			groupDB.create(new Group("/B"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> groupDB.delete("/B"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void usedGroupInGroupsRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new Group("/B"), 
				new Group("/ZZZ"), 
				groupDB);
		
		assertThat(afterDependencyRename.getGroupParams().get(0).getGroupPath()).isEqualTo("/ZZZ");
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
			
			assertThat(afterDependencyRename.getGroupParams().get(0).getGroupPath()).isEqualTo("/ZZZ/sub");
		});
	}
	
	@Test
	public void usedAttrTypeRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			atDB.create(new AttributeType("email", "foo"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> atDB.delete("email"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void usedAttrTypeRenameIsPropagated()
	{
		T afterDependencyRename = renameTest(
				new AttributeType("email", "foo"), 
				new AttributeType("changed", "foo"), 
				atDB);
		
		assertThat(afterDependencyRename.getAttributeParams().get(0).getAttributeType()).isEqualTo("changed");
	}

	
	@Test
	public void usedMsgTmplRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			msgTplDB.create(new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
					new I18nString("")), "consumer", MessageType.PLAIN, "channel"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> msgTplDB.delete("template"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
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
		
		assertThat(afterDependencyRename.getNotificationsConfiguration().getAcceptedTemplate()).isEqualTo("changed");
	}
	
	@Test
	public void usedMsgTmplUpdateToIncompatibleIsRestricted()
	{
		tx.runInTransaction(() -> {
			msgTplDB.create(new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
					new I18nString("")), SubmitRegistrationTemplateDef.NAME, MessageType.PLAIN, "channel"));
			
			T obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> msgTplDB.update(new MessageTemplate("template", "", new I18nMessage(new I18nString(""),
					new I18nString("")), "CHANGED-CONSUMER", MessageType.PLAIN, "channel")));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
}
