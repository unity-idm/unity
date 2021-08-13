/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.registration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.signupAndEnquiry.formfill.AdminRegistrationFormLauncher;
import io.imunity.webconsole.signupAndEnquiry.invitations.NewInvitationView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Shows all registration forms.
 * 
 * @author P.Piernik
 *
 */
public class RegistrationFormsComponent extends CustomComponent
{
	private MessageSource msg;
	private RegistrationFormsController controller;
	private GridWithActionColumn<RegistrationForm> registrationFormsList;
	private AdminRegistrationFormLauncher adminRegistrationFormLauncher;

	public RegistrationFormsComponent(MessageSource msg, RegistrationFormsController controller,
			AdminRegistrationFormLauncher adminRegistrationFormLauncher)

	{
		this.msg = msg;
		this.controller = controller;
		this.adminRegistrationFormLauncher = adminRegistrationFormLauncher;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
						e -> NavigationHelper.goToView(NewRegistrationFormView.VIEW_NAME)));

		registrationFormsList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		registrationFormsList.addHamburgerActions(getHamburgerActionsHandlers());
		registrationFormsList
				.addComponentColumn(
						f -> StandardButtonsHelper.buildLinkButton(f.getName(),
								e -> gotoEdit(f)),
						msg.getMessage("RegistrationFormsComponent.nameCaption"), 10)
				.setSortable(true).setComparator((r1, r2) -> {
					return r1.getName().compareTo(r2.getName());
				});
		;
		registrationFormsList.addComponentColumn(form -> {
			if (!form.isPubliclyAvailable())
				return null;
			Link link = new Link();
			String linkURL = controller.getPublicFormLink(form);
			link.setCaption(linkURL);
			link.setTargetName("_blank");
			link.setResource(new ExternalResource(linkURL));
			return link;
		}, msg.getMessage("RegistrationFormsComponent.linkCaption"), 20);

		registrationFormsList.setItems(getRegistrationForms());

		VerticalLayout main = new VerticalLayout();
		Label regCaption = new Label(msg.getMessage("RegistrationFormsComponent.caption"));
		regCaption.setStyleName(Styles.sectionTitle.toString());
		main.addComponent(regCaption);
		main.addComponent(buttonsBar);
		main.addComponent(registrationFormsList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private List<SingleActionHandler<RegistrationForm>> getActionsHandlers()
	{
		SingleActionHandler<RegistrationForm> edit = SingleActionHandler
				.builder4Edit(msg, RegistrationForm.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		return Arrays.asList(edit);

	}

	private List<SingleActionHandler<RegistrationForm>> getHamburgerActionsHandlers()
	{

		SingleActionHandler<RegistrationForm> invite = SingleActionHandler.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.invite"))
				.withIcon(Images.envelope_open.getResource())
				.withDisabledPredicate(form -> !(form.getRegistrationCode() == null
						&& form.isPubliclyAvailable()))
				.withHandler(items -> gotoInvitation(items.iterator().next())).build();

		SingleActionHandler<RegistrationForm> createRequest = SingleActionHandler
				.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.createRequest"))
				.withIcon(Images.file_add.getResource())
				.withHandler(items -> createRequest(items.iterator().next())).build();

		SingleActionHandler<RegistrationForm> clone = SingleActionHandler.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.clone"))
				.withIcon(Images.copy.getResource())
				.withHandler(items -> clone(items.iterator().next())).build();

		SingleActionHandler<RegistrationForm> remove = SingleActionHandler
				.builder4Delete(msg, RegistrationForm.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(invite, createRequest, clone, remove);

	}

	private void createRequest(RegistrationForm form)
	{
		adminRegistrationFormLauncher.showRegistrationDialog(form,
				RemotelyAuthenticatedPrincipal.getLocalContext(), TriggeringMode.manualAdmin,
				RegistrationFormsComponent.this::handleError);
	}

	private void handleError(Exception error)
	{
		NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorShowFormEdit"), error);
	}

	private void clone(RegistrationForm form)
	{
		NavigationHelper.goToView(NewRegistrationFormView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + form.getName());
	}

	private void gotoInvitation(RegistrationForm form)
	{
		NavigationHelper.goToView(NewInvitationView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ form.getName() + "&" + CommonViewParam.type.toString() + "="
				+ InvitationType.REGISTRATION.toString());
	}

	private void gotoEdit(RegistrationForm form)
	{
		NavigationHelper.goToView(EditRegistrationFormView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + form.getName());
	}

	private Collection<RegistrationForm> getRegistrationForms()
	{
		try
		{
			return controller.getRegistrationForms();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private void remove(RegistrationForm form, boolean dropRequests)
	{
		try
		{
			controller.removeRegistrationForm(form, dropRequests);
			registrationFormsList.removeElement(form);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(RegistrationForm form)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, Sets.newHashSet(form));

		new ConfirmWithOptionDialog(msg,
				msg.getMessage("RegistrationFormsComponent.confirmDelete", confirmText),
				msg.getMessage("RegistrationFormsComponent.dropRequests"),
				new ConfirmWithOptionDialog.Callback()
				{
					@Override
					public void onConfirm(boolean dropRequests)
					{

						remove(form, dropRequests);

					}
				}).show();
	}
}
