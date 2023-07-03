/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.credentials.sms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.elements.TextFieldWithVerifyButton;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.verifiable.VerifiableMobileNumber;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredential;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialExtraInfo;
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider;
import pl.edu.icm.unity.stdext.utils.MobileNumberUtils;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

import java.util.*;

public class SMSCredentialEditor implements CredentialEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SMSCredentialEditor.class);
	
	private enum CredentialSource
	{
		New, Existing
	}
	
	private MessageSource msg;
	private AttributeTypeSupport attrTypeSupport;
	private AttributeSupport attrSup;
	private ConfirmationInfoFormatter formatter;
	private MobileNumberConfirmationManager  mobileConfirmationMan;
	
	private ComboBox<String> currentMobileAttr;
	private SMSCredential helper;
	private RadioButtonGroup<CredentialSource> credentialSource;
	private TextFieldWithVerifyButton editor;
	private ConfirmationInfo confirmationInfo;
	private boolean skipUpdate = false;
	private CredentialEditorContext context;
	private SingleStringFieldBinder binder;
	private NotificationPresenter notificationPresenter;

	public SMSCredentialEditor(MessageSource msg, AttributeTypeSupport attrTypeSupport,
	                           AttributeSupport attrSup,
	                           MobileNumberConfirmationManager mobileConfirmationMan,
	                           ConfirmationInfoFormatter formatter)
	{
		this.msg = msg;
		this.attrTypeSupport = attrTypeSupport;
		this.attrSup = attrSup;
		this.mobileConfirmationMan = mobileConfirmationMan;
		this.formatter = formatter;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		binder =  new SingleStringFieldBinder(msg);
		this.context = context;
		helper = new SMSCredential();
		helper.setSerializedConfiguration(JsonUtil.parse(context.getCredentialConfiguration()));

		ComponentsContainer ret = new ComponentsContainer();

		credentialSource = new RadioButtonGroup<>();
		credentialSource.setItems(CredentialSource.New, CredentialSource.Existing);
		Map<CredentialSource, String> captions = new HashMap<>();
		captions.put(CredentialSource.New, msg.getMessage("SMSCredentialEditor.newValue"));
		captions.put(CredentialSource.Existing,
				msg.getMessage("SMSCredentialEditor.existingValue"));
		credentialSource.setItemLabelGenerator(captions::get);

		credentialSource.addValueChangeListener(e -> {
			if (e.getValue().equals(CredentialSource.New))
			{
				editor.setVisible(true);
				currentMobileAttr.setVisible(false);
			} else
			{
				editor.setVisible(false);
				currentMobileAttr.setVisible(true);
			}
		});

		ret.add(credentialSource);

		currentMobileAttr = new ComboBox<>();
		currentMobileAttr.setLabel(msg.getMessage("SMSCredentialEditor.newMobileNumber"));
		currentMobileAttr.setRequired(true);
		currentMobileAttr.setRequiredIndicatorVisible(true);
		currentMobileAttr.setAllowCustomValue(false);
		
		List<String> userMobiles = getUserMobiles(context.getEntityId());
		if (!userMobiles.isEmpty())
		{
			currentMobileAttr.setItems(userMobiles);
			currentMobileAttr.setValue(userMobiles.get(0));
			ret.add(currentMobileAttr);
		}

		confirmationInfo = new ConfirmationInfo();
		editor = new TextFieldWithVerifyButton(context.isAdminMode(), 
				msg.getMessage("SMSCredentialEditor.verify"),
				VaadinIcon.MOBILE_RETRO.create(),
				msg.getMessage("SMSCredentialEditor.confirmedCheckbox"),
				context.isShowLabelInline());
		editor.setLabel(msg.getMessage("SMSCredentialEditor.newMobileNumber"));

		editor.addVerifyButtonClickListener(e -> {

			String value = editor.getValue();
			String error = MobileNumberUtils.validate(value);
			if (error != null)
			{
				editor.setComponentError(value + ":" + error);
				return;
			} else
			{
				editor.setComponentError(null);
			}

			MobileNumberConfirmationDialog confirmationDialog = new MobileNumberConfirmationDialog(
					value, confirmationInfo, msg, mobileConfirmationMan,
					helper.getMobileNumberConfirmationConfiguration().get(),
					() -> updateConfirmationStatusIconAndButtons(), notificationPresenter);
			confirmationDialog.open();
		});

		editor.addAdminConfirmCheckBoxValueChangeListener(e -> {
			if (!skipUpdate)
			{
				confirmationInfo = new ConfirmationInfo(e.getValue());
				updateConfirmationStatusIconAndButtons();
			}
		});

		editor.addTextFieldValueChangeListener(e -> {
			confirmationInfo = new ConfirmationInfo();
			updateConfirmationStatusIconAndButtons();
		});
		updateConfirmationStatusIconAndButtons();
		ret.add(editor);
		credentialSource.setItemEnabledProvider(i -> {
			if (i.equals(CredentialSource.Existing) && userMobiles.isEmpty())
				return false;
			return true;
		});

		if (!userMobiles.isEmpty())
		{
			credentialSource.setValue(CredentialSource.Existing);
		} else
		{

			credentialSource.setValue(CredentialSource.New);
			
			if (context.getEntityId() == null)
			{
				credentialSource.setVisible(false);
			}
		}
		
		if (context.isCustomWidth())
			editor.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		if(context.isRequired())
			editor.getElement().setProperty("title", msg.getMessage("fieldRequired"));

		binder.forField(editor, context.isRequired()).bind("value");
		binder.setBean(new StringBindingValue(""));
		return ret;
	}

	private void updateConfirmationStatusIconAndButtons()
	{
		editor.setConfirmationStatusIcon(
				formatter.getSimpleConfirmationStatusString(confirmationInfo),
				confirmationInfo.isConfirmed());
		editor.setVerifyButtonVisible(
				!confirmationInfo.isConfirmed() && !editor.getValue().isEmpty());
		skipUpdate = true;
		editor.setAdminCheckBoxValue(confirmationInfo.isConfirmed());
		skipUpdate = false;
	}

	private List<String> getUserMobiles(Long entityId)
	{

		List<String> ret = new ArrayList<>();
		if (entityId == null)
			return ret;
		
		AttributeExt attributeByMetadata = null;
		try
		{
			attributeByMetadata = attrSup.getAttributeByMetadata(
					new EntityParam(entityId), "/",
					ContactMobileMetadataProvider.NAME);
		} catch (EngineException e)
		{
			log.error("Can not get attribute for entity " + entityId + " with meta "
					+ ContactMobileMetadataProvider.NAME, e);
		}

		if (attributeByMetadata == null)
			return ret;

		AttributeType type = attrTypeSupport.getType(attributeByMetadata);
		AttributeValueSyntax<?> syntax = attrTypeSupport.getSyntax(type);
		if (syntax.getValueSyntaxId().equals(VerifiableMobileNumberAttributeSyntax.ID))
		{

			for (String value : attributeByMetadata.getValues())
			{
				VerifiableMobileNumber vmobile = (VerifiableMobileNumber) syntax
						.convertFromString(value);
				if (vmobile.isConfirmed())
					ret.add(vmobile.getValue());
			}
		}

		return ret;
	}

	@Override
	public Optional<Component> getViewer(String credentialInfo)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setPadding(false);

		SMSCredentialExtraInfo pei = SMSCredentialExtraInfo.fromJson(credentialInfo);
		if (pei.getLastChange() == null)
			return Optional.empty();

		ret.add(new Label(msg.getMessage("SMSCredentialEditor.lastModification",
				pei.getLastChange())));
		ret.add(new Label(msg.getMessage("SMSCredentialEditor.selectedMobileNumber",
				hideMobile(pei.getMobile()))));
		return Optional.of(ret);
	}
	
	private String hideMobile(String mobile)
	{
		StringBuilder builder = new StringBuilder();
		if (mobile == null)
			return builder.toString();

		if (mobile.length() <= 5)
		{
			builder.append(mobile);

		} else
		{
			builder.append(mobile.substring(0, 3));
			for (int i = 3; i < mobile.length() - 2; i++)
			{
				builder.append("*");
			}
			builder.append(mobile.substring(mobile.length() - 2));
		}
		return builder.toString();
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		String mobile;
		if (credentialSource.getValue().equals(CredentialSource.Existing))
		{
			mobile = currentMobileAttr.getValue();
		} else
		{
			if (confirmationInfo.isConfirmed())
			{
				mobile = editor.getValue();
			} else
			{
				editor.setComponentError(msg.getMessage("SMSCredentialEditor.onlyConfirmedValue"));
				throw new IllegalCredentialException(msg.getMessage(
						"SMSCredentialEditor.onlyConfirmedValue"));
			}

			String error = MobileNumberUtils.validate(mobile);
			if (error != null)
			{
				editor.setComponentError(mobile + ":" + error);
				throw new IllegalCredentialException(mobile + ":" + error);
			}
		}

		if (context.isRequired() && mobile != null && mobile.isEmpty())
		{
			editor.setComponentError(msg.getMessage("fieldRequired"));
			currentMobileAttr.setErrorMessage(msg.getMessage("fieldRequired"));
			throw new IllegalCredentialException(msg.getMessage("fieldRequired"));
		} else
		{
			editor.setComponentError(null);
			currentMobileAttr.setErrorMessage(null);
		}

		return mobile;
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		if (error == null)
		{
			currentMobileAttr.setErrorMessage(null);
			editor.setValue("");
			editor.setComponentError(null);
			return;
		}

		String message = error.getMessage();
		editor.setComponentError(message);
		currentMobileAttr.setErrorMessage(message);
		currentMobileAttr.setInvalid(true);
	}
}
