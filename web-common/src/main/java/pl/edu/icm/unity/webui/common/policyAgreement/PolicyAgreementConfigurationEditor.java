/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfigTextParser;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements.Editor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Allows for edit single policy agreement entry. Used in
 * {@link ListOfDnDCollapsableElements}
 * 
 * @author P.Piernik
 *
 */
public class PolicyAgreementConfigurationEditor extends Editor<PolicyAgreementConfiguration>
{
	private UnityMessageSource msg;
	private Collection<PolicyDocumentWithRevision> policyDocuments;
	private Binder<PolicyAgreementConfigurationVaadinBean> binder;
	private FormLayout main;
	private ChipsWithDropdown<PolicyDocumentWithRevision> policyToAccept;

	public PolicyAgreementConfigurationEditor(UnityMessageSource msg,
			Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		this.msg = msg;
		this.policyDocuments = policyDocuments;
		init();
	}

	private void init()
	{
		binder = new Binder<>(PolicyAgreementConfigurationVaadinBean.class);
		main = new FormLayout();

		policyToAccept = new ChipsWithDropdown<>(p -> p.name, true);
		policyToAccept.setCaption(msg.getMessage("PolicyAgreementConfigEditor.documentsToAccept"));
		policyToAccept.setItems(policyDocuments.stream().collect(Collectors.toList()));

		EnumComboBox<PolicyAgreementPresentationType> presentationType = new EnumComboBox<PolicyAgreementPresentationType>(
				msg, "PolicyAgreementPresentationType.", PolicyAgreementPresentationType.class,
				PolicyAgreementPresentationType.INFORMATIVE_ONLY);
		presentationType.setCaption(msg.getMessage("PolicyAgreementConfigEditor.presentationType"));
		I18nTextField text = new I18nTextField(msg, msg.getMessage("PolicyAgreementConfigEditor.text"));
		text.setWidth(100, Unit.PERCENTAGE);
		HorizontalLayout buttons = new HorizontalLayout();
		HorizontalLayout buttonsWrapper = new HorizontalLayout();
		buttonsWrapper.setMargin(false);
		buttonsWrapper.addComponent(buttons);
		buttonsWrapper.setWidth(100, Unit.PERCENTAGE);
		buttonsWrapper.setComponentAlignment(buttons, Alignment.BOTTOM_RIGHT);

		policyToAccept.addValueChangeListener(e -> {
			buttons.removeAllComponents();
			e.getValue().forEach(d -> {
				Button b = new Button();
				b.setCaption(d.name);
				b.addStyleName(Styles.vButtonSmall.toString());
				b.addClickListener(
						eb -> text.insertOnFocused(prepareToInsert(d.name, d.displayedName)));
				buttons.addComponent(b);
			});
		});

		main.addComponent(policyToAccept);
		main.addComponent(presentationType);
		main.addComponent(buttonsWrapper);
		main.addComponent(text);

		binder = new Binder<>(PolicyAgreementConfigurationVaadinBean.class);
		binder.forField(policyToAccept).withValidator((v, c) -> validatePolicyToAccept(v))
				.bind("documentsToAccept");
		binder.forField(presentationType).bind("presentationType");
		binder.forField(text).withValidator((v, c) -> validateText(v)).bind("text");
		binder.setBean(new PolicyAgreementConfigurationVaadinBean());
		binder.addValueChangeListener(e -> fireEvent(new ValueChangeEvent<>(this, getValue(), true)));
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
					PolicyAgreementConfigTextParser.convertTextToConfig(policyDocuments, text)).keySet();
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

	private I18nString prepareToInsert(String name, I18nString dispName)
	{
		I18nString ret = new I18nString(name);
		for (String locale : msg.getEnabledLocales().values().stream().map(l -> l.toString())
				.collect(Collectors.toList()))
		{
			String dname = dispName != null ? dispName.getValueRaw(locale) : null;
			ret.addValue(locale,
					"{" + name + ":" + (dname == null || dname.isEmpty() ? name : dname) + "}");
		}
		return ret;
	}

	@Override
	protected Component initContent()
	{

		return main;
	}

	@Override
	protected void doSetValue(PolicyAgreementConfiguration value)
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
			throw new FormValidationException(msg.getMessage("PolicyAgreementConfigEditor.invalidConfiguration"));
		}	
	}

	@Override
	protected String getHeaderText()
	{
		return policyToAccept.getValue() == null || policyToAccept.getValue().isEmpty()
				? msg.getMessage("empty")
				: String.join(", ", policyToAccept.getValue().stream().map(p -> p.name)
						.collect(Collectors.toList()));

	}

	public static class PolicyAgreementConfigurationVaadinBean
	{
		private List<PolicyDocumentWithRevision> documentsToAccept;
		private PolicyAgreementPresentationType presentationType;
		private I18nString text;

		public PolicyAgreementConfigurationVaadinBean()
		{
			documentsToAccept = new ArrayList<>();
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
