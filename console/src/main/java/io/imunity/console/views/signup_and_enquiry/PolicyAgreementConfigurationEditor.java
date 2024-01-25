/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.elements.FocusedField;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfigTextParser;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;


public class PolicyAgreementConfigurationEditor extends CollapsableGrid.Editor<PolicyAgreementConfiguration>
{
	private final MessageSource msg;
	private final Collection<PolicyDocumentWithRevision> policyDocuments;
	private Binder<PolicyAgreementConfigurationVaadinBean> binder;
	private MultiSelectComboBox<PolicyDocumentWithRevision> policyToAccept;
	private final FocusedField focussedField = new FocusedField();
	private List<Button> buttons;

	public PolicyAgreementConfigurationEditor(MessageSource msg,
			Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		this.msg = msg;
		this.policyDocuments = policyDocuments;
		init();
	}

	private void init()
	{
		binder = new Binder<>(PolicyAgreementConfigurationVaadinBean.class);
		buttons = new ArrayList<>();
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		main.addFormItem(new Span(msg.getMessage("PolicyAgreementConfigEditor.noPolicyDocuments")),
				msg.getMessage("PolicyAgreementConfigEditor.documentsToAccept"))
				.setVisible(policyDocuments.isEmpty());

		policyToAccept = new MultiSelectComboBox<>();
		policyToAccept.setItemLabelGenerator(p -> p.name);
		policyToAccept.setItems(new ArrayList<>(policyDocuments));
		policyToAccept.setWidth(TEXT_FIELD_BIG.value());
		main.addFormItem(policyToAccept, msg.getMessage("PolicyAgreementConfigEditor.documentsToAccept"))
				.setVisible(!policyDocuments.isEmpty());

		Select<PolicyAgreementPresentationType> presentationType = new Select<>();
		presentationType.setItemLabelGenerator(item -> msg.getMessage("PolicyAgreementPresentationType." + item));
		presentationType.setItems(PolicyAgreementPresentationType.values());
		presentationType.setValue(PolicyAgreementPresentationType.INFORMATIVE_ONLY);
		presentationType.setWidth(TEXT_FIELD_MEDIUM.value());
		main.addFormItem(presentationType, msg.getMessage("PolicyAgreementConfigEditor.presentationType"));

		LocalizedTextFieldDetails text = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		text.setWidthFull();
		text.setWidth(TEXT_FIELD_BIG.value());
		text.addValuesChangeListener(focussedField::set);


		HorizontalLayout buttonsLayout = new HorizontalLayout();
		HorizontalLayout buttonsWrapper = new HorizontalLayout();
		buttonsWrapper.setPadding(false);
		Span vars = new Span(msg.getMessage("PolicyAgreementConfigEditor.variables"));
		buttonsWrapper.add(vars, buttonsLayout);
		buttonsWrapper.setVisible(false);
		
		policyToAccept.addValueChangeListener(e -> {
			buttonsLayout.removeAll();
			e.getValue().forEach(d -> {
				Button button = new Button(d.name);
				button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
				button.addClickListener(event ->
				{
					if (focussedField.isSet())
						addVar(d.name, d.displayedName);
				});
				buttonsLayout.add(button);
				buttons.add(button);
			});
			buttonsWrapper.setVisible(buttonsLayout.getComponentCount() != 0);	
		});
		
		VerticalLayout buttonsAndTextWrapper = new VerticalLayout();
		buttonsAndTextWrapper.setPadding(false);
		buttonsAndTextWrapper.setSpacing(false);
		buttonsAndTextWrapper.add(buttonsWrapper, text);
		buttonsAndTextWrapper.add(text);

		main.addFormItem(buttonsAndTextWrapper, msg.getMessage("PolicyAgreementConfigEditor.text"));
		add(main);

		binder = new Binder<>(PolicyAgreementConfigurationVaadinBean.class);
		binder.forField(policyToAccept)
				.withConverter(List::copyOf, HashSet::new)
				.withValidator((v, c) -> validatePolicyToAccept(v))
				.bind(PolicyAgreementConfigurationVaadinBean::getDocumentsToAccept, PolicyAgreementConfigurationVaadinBean::setDocumentsToAccept);
		binder.forField(presentationType)
				.bind(PolicyAgreementConfigurationVaadinBean::getPresentationType, PolicyAgreementConfigurationVaadinBean::setPresentationType);
		binder.forField(text)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.withValidator((v, c) -> validateText(v))
				.bind(PolicyAgreementConfigurationVaadinBean::getText, PolicyAgreementConfigurationVaadinBean::setText);
		binder.setBean(new PolicyAgreementConfigurationVaadinBean());
		binder.addValueChangeListener(e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), false)));
	}

	private void addVar(String name, I18nString dispName)
	{
		String v = focussedField.getValue();
		String st = v.substring(0, focussedField.getCursorPosition());
		String fi = v.substring(focussedField.getCursorPosition());
		String dname = dispName != null ? dispName.getValueRaw(focussedField.getLocale()) : null;
		focussedField.setValue(st + "{" + name + ":" + (dname == null || dname.isEmpty() ? name : dname) + "}" + fi);
	}

	private ValidationResult validatePolicyToAccept(List<PolicyDocumentWithRevision> value)
	{
		if (value != null && !value.isEmpty() && !value.stream().map(sv -> sv.mandatory)
				.allMatch(Boolean.valueOf(value.get(0).mandatory)::equals))
		{
			return ValidationResult
					.error(msg.getMessage("PolicyAgreementConfigEditor.mixedDocumentObliatory"));
		}
		return ValidationResult.ok();
	}

	private ValidationResult validateText(I18nString text)
	{
		Set<Long> searchAllDocIds;
		try
		{
			searchAllDocIds = PolicyAgreementConfigTextParser.getAllDocsPlaceholdersInConfigText(
					PolicyAgreementConfigTextParser.convertTextToConfig(policyDocuments, text))
					.keySet();
		} catch (Exception e1)
		{
			return ValidationResult.error(msg.getMessage("PolicyAgreementConfigEditor.invalidText"));
		}
		if (!policyToAccept.getValue().stream().map(p -> p.id).collect(Collectors.toSet())
				.containsAll(searchAllDocIds))
		{
			return ValidationResult
					.error(msg.getMessage("PolicyAgreementConfigEditor.notSelectedDocument"));
		}
		return ValidationResult.ok();
	}

	@Override
	public void setValue(PolicyAgreementConfiguration value)
	{
		PolicyAgreementConfigurationVaadinBean bean = new PolicyAgreementConfigurationVaadinBean();
		bean.setDocumentsToAccept(policyDocuments.stream()
				.filter(d -> value.documentsIdsToAccept.contains(d.id)).collect(Collectors.toList()));
		bean.setPresentationType(value.presentationType);
		bean.setText(PolicyAgreementConfigTextParser.convertTextToPresentation(policyDocuments, value.text));
		binder.setBean(bean);
	}

	@Override
	public PolicyAgreementConfiguration getValue()
	{
		if (binder.validate().hasErrors())
		{
			return null;
		}

		PolicyAgreementConfigurationVaadinBean bean = binder.getBean();

		return new PolicyAgreementConfiguration(
				bean.documentsToAccept.stream().map(d -> d.id).collect(Collectors.toList()),
				bean.presentationType,
				PolicyAgreementConfigTextParser.convertTextToConfig(policyDocuments, bean.text));

	}

	@Override
	protected void validate() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException(
					msg.getMessage("PolicyAgreementConfigEditor.invalidConfiguration"));
		}
	}

	@Override
	protected String getHeaderText()
	{
		return policyToAccept.getValue() == null || policyToAccept.getValue().isEmpty()
				? msg.getMessage("empty")
				: policyToAccept.getValue().stream().map(p -> p.name)
						.collect(Collectors.joining(", "));

	}

	@Override
	protected PolicyAgreementConfiguration generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(PolicyAgreementConfiguration policyAgreementConfiguration)
	{
		setValue(policyAgreementConfiguration);
	}

	public static class PolicyAgreementConfigurationVaadinBean
	{
		private List<PolicyDocumentWithRevision> documentsToAccept;
		private PolicyAgreementPresentationType presentationType;
		private I18nString text;

		public PolicyAgreementConfigurationVaadinBean()
		{
			documentsToAccept = new ArrayList<>();
			text = new I18nString();
		}

		PolicyAgreementConfigurationVaadinBean(List<PolicyDocumentWithRevision> documentsToAccept,
				PolicyAgreementPresentationType presentationType, I18nString text)
		{
			this.documentsToAccept = documentsToAccept;
			this.presentationType = presentationType;
			this.text = text;
		}

		public List<PolicyDocumentWithRevision> getDocumentsToAccept()
		{
			return documentsToAccept;
		}

		public void setDocumentsToAccept(List<PolicyDocumentWithRevision> documentsToAccept)
		{
			this.documentsToAccept = documentsToAccept;
		}

		public PolicyAgreementPresentationType getPresentationType()
		{
			return presentationType;
		}

		public void setPresentationType(PolicyAgreementPresentationType presentationType)
		{
			this.presentationType = presentationType;
		}

		public I18nString getText()
		{
			return text;
		}

		public void setText(I18nString text)
		{
			this.text = text;
		}
	}
}
