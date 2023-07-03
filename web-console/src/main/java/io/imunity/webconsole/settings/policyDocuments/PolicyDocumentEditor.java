/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.policyDocuments;

import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.LabelAsTextField;
import pl.edu.icm.unity.webui.common.i18n.I18nOptionalLangRichTextField;
import pl.edu.icm.unity.webui.common.i18n.I18nOptionalLangTextField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

class PolicyDocumentEditor extends CustomComponent
{
	private MessageSource msg;
	private Binder<PolicyDocumentVaadinBean> binder;
	private CustomField<I18nString> content;
	private FormLayout mainLayout;

	PolicyDocumentEditor(MessageSource msg, PolicyDocumentWithRevision toEdit, Set<String> allNames)
	{
		this.msg = msg;
		init(toEdit, allNames);
	}

	private void init(PolicyDocumentWithRevision toEdit, Set<String> allNames)
	{
		TextField name = new TextField(msg.getMessage("PolicyDocumentEditor.name"));
		I18nTextField displayedName = new I18nTextField(msg,
				msg.getMessage("PolicyDocumentEditor.displayedName"));
		displayedName.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		CheckBox optional = new CheckBox(msg.getMessage("PolicyDocumentEditor.optionalAcceptance"));
		LabelAsTextField revision = new LabelAsTextField(msg.getMessage("PolicyDocumentEditor.revision"));
		revision.setReadOnly(true);
		EnumComboBox<PolicyDocumentContentType> type = new EnumComboBox<>(
				msg.getMessage("PolicyDocumentEditor.contentType"), msg, "PolicyDocumentType.",
				PolicyDocumentContentType.class, PolicyDocumentContentType.EMBEDDED);
		type.setEmptySelectionAllowed(false);
		type.setValue(null);
		type.addValueChangeListener(e -> {
			content.setValue(new I18nString());
			changeType(e.getValue());
		});

		content = new I18nOptionalLangRichTextField(msg, msg.getMessage("PolicyDocumentEditor.text"));

		binder = new Binder<>(PolicyDocumentVaadinBean.class);
		binder.forField(name).withValidator((s, c) -> {
			if (allNames.contains(s))
			{
				return ValidationResult.error(msg.getMessage("PolicyDocumentEditor.nameExists"));
			} else
			{
				return ValidationResult.ok();
			}

		}).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.forField(displayedName).bind("displayedName");
		binder.forField(optional).bind("optional");
		binder.forField(revision).withConverter(new StringToIntegerConverter("")).bind("revision");
		binder.forField(type).bind("contentType");

		mainLayout = new FormLayout();
		mainLayout.setMargin(false);
		mainLayout.addComponent(name);
		mainLayout.addComponent(displayedName);
		mainLayout.addComponent(optional);
		mainLayout.addComponent(revision);
		mainLayout.addComponent(type);
		mainLayout.addComponent(content);

		setCompositionRoot(mainLayout);

		binder.setBean(toEdit != null ? new PolicyDocumentVaadinBean(toEdit) : new PolicyDocumentVaadinBean());
	}

	private void changeType(PolicyDocumentContentType value)
	{
		mainLayout.removeComponent(content);
		if (binder.getBinding("content").isPresent())
		{
			binder.removeBinding("content");
		}

		if (value.equals(PolicyDocumentContentType.EMBEDDED))
		{
			content = new I18nOptionalLangRichTextField(msg, msg.getMessage("PolicyDocumentEditor.text"));
			binder.forField(content).withValidator((val, context) -> ValidationResult.ok()).bind("content");
		} else
		{
			content = new I18nOptionalLangTextField(msg, msg.getMessage("PolicyDocumentEditor.url"));
			binder.forField(content).withValidator((val, context) -> {
				if (val != null)
				{
					for (String v : val.getMap().values())
						if (v != null && !v.isEmpty() && !URIHelper.isWebReady(v))
						{
							return ValidationResult
									.error(msg.getMessage("FileField.notWebUri"));
						}
				}

				return ValidationResult.ok();

			}).bind("content");
		}
		mainLayout.addComponent(content);
	}

	boolean hasErrors()
	{
		return binder.validate().hasErrors();
	}

	PolicyDocumentUpdateRequest getUpdateRequest()
	{
		PolicyDocumentVaadinBean bean = binder.getBean();
		return new PolicyDocumentUpdateRequest(bean.getId(), bean.getName(), bean.getDisplayedName(),
				!bean.isOptional(), bean.getContentType(), bean.getContent());

	}

	PolicyDocumentCreateRequest getCreateRequest()
	{
		PolicyDocumentVaadinBean bean = binder.getBean();
		return new PolicyDocumentCreateRequest(bean.getName(), bean.getDisplayedName(), bean.isOptional(),
				bean.getContentType(), bean.getContent());
	}

	public static class PolicyDocumentVaadinBean
	{
		private long id;
		private String name;
		private I18nString displayedName;
		private int revision;
		private boolean optional;
		private PolicyDocumentContentType contentType;
		private I18nString content;

		public PolicyDocumentVaadinBean()
		{
			contentType = PolicyDocumentContentType.EMBEDDED;
			revision = 1;
		}

		public PolicyDocumentVaadinBean(PolicyDocumentWithRevision bean)
		{
			this.id = bean.id;
			this.name = bean.name;
			this.content = bean.content;
			this.displayedName = bean.displayedName;
			this.optional = !bean.mandatory;
			this.revision = bean.revision;
			this.contentType = bean.contentType;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public I18nString getDisplayedName()
		{
			return displayedName;
		}

		public void setDisplayedName(I18nString displayedName)
		{
			this.displayedName = displayedName;
		}

		public int getRevision()
		{
			return revision;
		}

		public void setRevision(int revision)
		{
			this.revision = revision;
		}

		public PolicyDocumentContentType getContentType()
		{
			return contentType;
		}

		public void setContentType(PolicyDocumentContentType contentType)
		{
			this.contentType = contentType;
		}

		public I18nString getContent()
		{
			return content;
		}

		public void setContent(I18nString content)
		{
			this.content = content;
		}

		public long getId()
		{
			return id;
		}

		public boolean isOptional()
		{
			return optional;
		}

		public void setOptional(boolean optional)
		{
			this.optional = optional;
		}
	}
}
