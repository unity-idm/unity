/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.x500.X500Name;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * View for edit SAML individual trusted SP
 * 
 * @author P.Piernik
 *
 */
class EditIndividualTrustedSPSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private Binder<SAMLIndividualTrustedSPConfiguration> configBinder;
	private boolean editMode = false;
	private Set<String> certificates;
	private Set<String> usedNames;

	EditIndividualTrustedSPSubView(MessageSource msg, UnityServerConfiguration serverConfig,
			URIAccessService uriAccessService, SAMLIndividualTrustedSPConfiguration toEdit,
			SubViewSwitcher subViewSwitcher, Set<String> usedNames, Set<String> certificates,
			Consumer<SAMLIndividualTrustedSPConfiguration> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.certificates = certificates;
		this.usedNames = usedNames;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		editMode = toEdit != null;

		configBinder = new Binder<>(SAMLIndividualTrustedSPConfiguration.class);
		FormLayout header = buildHeaderSection();
		CollapsibleLayout singleLogout = buildSingleLogoutSection();
		configBinder.setBean(editMode ? toEdit.clone() : new SAMLIndividualTrustedSPConfiguration());
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(singleLogout);
		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg,
						msg.getMessage("EditIndividualTrustedSPSubView.invalidConfiguration"),
						e);
			}
		};
		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		setCompositionRoot(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField name = new TextField(msg.getMessage("EditIndividualTrustedSPSubView.name"));
		name.focus();
		configBinder.forField(name).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator(msg)).withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg.getMessage(
								"EditIndividualTrustedSPSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind("name");
		header.addComponent(name);

		I18nTextField displayedName = new I18nTextField(msg,
				msg.getMessage("EditIndividualTrustedSPSubView.displayedName"));
		configBinder.forField(displayedName).bind("displayedName");
		header.addComponent(displayedName);

		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("EditIndividualTrustedSPSubView.logo"));
		logo.configureBinding(configBinder, "logo");
		header.addComponent(logo);

		CheckBox x500Name = new CheckBox(msg.getMessage("EditIndividualTrustedSPSubView.X500NameUse"));
		configBinder.forField(x500Name).bind("x500Name");
		header.addComponent(x500Name);

		TextField id = new TextField(msg.getMessage("EditIndividualTrustedSPSubView.id"));
		id.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((s, c) -> {
			if (x500Name.getValue())
			{
				try
				{
					new X500Name(s);
					return ValidationResult.ok();
				} catch (Exception e)
				{
					return ValidationResult.error(
							msg.getMessage("EditIndividualTrustedSPSubView.invalidX500Name"));
				}
			} else
			{
				return ValidationResult.ok();
			}
		}).bind("id");
		header.addComponent(id);

		ChipsWithDropdown<String> certificatesCombo = new ChipsWithDropdown<>();
		certificatesCombo.setCaption(msg.getMessage("EditIndividualTrustedSPSubView.certificates"));
		certificatesCombo.setItems(certificates.stream().collect(Collectors.toList()));
		configBinder.forField(certificatesCombo).bind("certificates");
		header.addComponent(certificatesCombo);

		CheckBox encryptAssertions = new CheckBox(
				msg.getMessage("EditIndividualTrustedSPSubView.encryptAssertions"));
		configBinder.forField(encryptAssertions).bind("encryptAssertions");
		header.addComponent(encryptAssertions);

		ChipsWithTextfield authorizedURIs = new ChipsWithTextfield(msg);
		authorizedURIs.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		authorizedURIs.setCaption(msg.getMessage("EditIndividualTrustedSPSubView.authorizedURIs"));
		configBinder.forField(authorizedURIs).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}
			return ValidationResult.ok();
		}

		).bind("authorizedRedirectsUri");
		header.addComponent(authorizedURIs);

		return header;
	}

	private CollapsibleLayout buildSingleLogoutSection()
	{
		FormLayoutWithFixedCaptionWidth singleLogout = new FormLayoutWithFixedCaptionWidth();
		singleLogout.setMargin(false);

		TextField postLogoutEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedSPSubView.postLogoutEndpoint"));
		postLogoutEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(postLogoutEndpoint).bind("postLogoutEndpoint");
		singleLogout.addComponent(postLogoutEndpoint);

		TextField postLogoutResponseEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedSPSubView.postLogoutResponseEndpoint"));
		postLogoutResponseEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(postLogoutResponseEndpoint).bind("postLogoutResponseEndpoint");
		singleLogout.addComponent(postLogoutResponseEndpoint);

		TextField redirectLogoutEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedSPSubView.redirectLogoutEndpoint"));
		redirectLogoutEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(redirectLogoutEndpoint).bind("redirectLogoutEndpoint");
		singleLogout.addComponent(redirectLogoutEndpoint);

		TextField redirectLogoutResponseEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedSPSubView.redirectLogoutResponseEndpoint"));
		redirectLogoutResponseEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(redirectLogoutResponseEndpoint).bind("redirectLogoutResponseEndpoint");
		singleLogout.addComponent(redirectLogoutResponseEndpoint);

		TextField soapLogoutEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedSPSubView.soapLogoutEndpoint"));
		soapLogoutEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(soapLogoutEndpoint).bind("soapLogoutEndpoint");
		singleLogout.addComponent(soapLogoutEndpoint);

		return new CollapsibleLayout(msg.getMessage("EditIndividualTrustedSPSubView.singleLogout"),
				singleLogout);
	}

	@Override
	public List<String> getBredcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditIndividualTrustedSPSubView.trustedSP"),
					configBinder.getBean().getName());
		else
			return Arrays.asList(msg.getMessage("EditIndividualTrustedSPSubView.newTrustedSP"));
	}

	private SAMLIndividualTrustedSPConfiguration getTrustedFederation() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean();
	}
}
