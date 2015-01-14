package pl.edu.icm.unity.webadmin.confirmation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.RequiredComboBox;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;

public class ConfirmationConfigurationEditor extends FormLayout
{

	private UnityMessageSource msg;
	private NotificationsManagement notificationsMan;
	private MessageTemplateManagement msgMan;
	private String forType;

	private ComboBox name;
	private ComboBox msgTemplate;
	private ComboBox notificationChannel;

	protected ConfirmationConfigurationEditor(UnityMessageSource msg,
			NotificationsManagement notificationsMan, AttributesManagement attrsMan,
			MessageTemplateManagement msgMan, String forType, List<String> names,
			ConfirmationConfiguration toEdit) throws EngineException
	{
		this.msg = msg;
		this.notificationsMan = notificationsMan;
		this.msgMan = msgMan;
		this.forType = forType;
		initUI(toEdit, names);
	}

	private void initUI(ConfirmationConfiguration toEdit, List<String> names)
			throws EngineException
	{
		boolean editMode = toEdit != null;

		name = new RequiredComboBox(
				msg.getMessage("ConfirmationConfigurationViewer.forName"), msg);
		name.setImmediate(true);
		name.setValidationVisible(false);
		name.setNullSelectionAllowed(false);

		if (names != null)
			for (String n : names)
			{
				name.addItem(n);
			}
		if (name.size() > 0)
		{
			name.setValue(name.getItemIds().toArray()[0]);
		}

		notificationChannel = new RequiredComboBox(
				msg.getMessage("ConfirmationConfigurationViewer.notificationChannel"),
				msg);
		notificationChannel.setImmediate(true);
		notificationChannel.setValidationVisible(false);
		notificationChannel.setNullSelectionAllowed(false);
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		for (String c : channels)
			notificationChannel.addItem(c);
		if (notificationChannel.size() > 0)
		{
			notificationChannel.setValue(notificationChannel.getItemIds().toArray()[0]);
		}

		msgTemplate = new CompatibleTemplatesComboBox(ConfirmationTemplateDef.NAME, msgMan);
		msgTemplate.setCaption(msg
				.getMessage("ConfirmationConfigurationViewer.msgTemplate"));
		msgTemplate.setValidationVisible(false);
		msgTemplate.setNullSelectionAllowed(false);
		msgTemplate.setRequired(true);
		msgTemplate.setRequiredError(msg.getMessage("fieldRequired"));
		msgTemplate.setImmediate(true);

		if (msgTemplate.size() > 0)
		{
			msgTemplate.setValue(msgTemplate.getItemIds().toArray()[0]);
		}

		if (editMode)
		{
			name.addItem(toEdit.getNameToConfirm());
			name.setValue(toEdit.getNameToConfirm());
			name.setReadOnly(true);
			msgTemplate.setValue(toEdit.getMsgTemplate());
			notificationChannel.setValue(toEdit.getNotificationChannel());
		}

		addComponents(name, msgTemplate, notificationChannel);
		setSizeFull();
	}

	public ConfirmationConfiguration getConfirmationConfiguration()
	{
		return new ConfirmationConfiguration(forType, name.getValue().toString(),
				notificationChannel.getValue().toString(), msgTemplate.getValue()
						.toString());
	}
}
