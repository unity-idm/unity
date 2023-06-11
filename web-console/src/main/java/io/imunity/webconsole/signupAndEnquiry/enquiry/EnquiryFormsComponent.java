/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.enquiry;

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

import io.imunity.webconsole.signupAndEnquiry.formfill.AdminEnquiryFormLauncher;
import io.imunity.webconsole.signupAndEnquiry.invitations.NewInvitationView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Shows all enquiry forms
 * 
 * @author P.Piernik
 *
 */
public class EnquiryFormsComponent extends CustomComponent
{
	private MessageSource msg;
	private EnquiryFormsController controller;
	private AdminEnquiryFormLauncher adminEnquiryFormLauncher;

	private GridWithActionColumn<EnquiryForm> enquiryFormsList;

	public EnquiryFormsComponent(MessageSource msg, EnquiryFormsController controller,
			AdminEnquiryFormLauncher adminEnquiryFormLauncher)
	{
		this.msg = msg;
		this.controller = controller;
		this.adminEnquiryFormLauncher = adminEnquiryFormLauncher;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewEnquiryFormView.VIEW_NAME)));

		enquiryFormsList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		enquiryFormsList.addHamburgerActions(getHamburgerActionsHandlers());
		enquiryFormsList.addComponentColumn(
				f -> StandardButtonsHelper.buildLinkButton(f.getName(), e -> gotoEdit(f)),
				msg.getMessage("EnquiryFormsComponent.nameCaption"), 10).setSortable(true)
				.setComparator((e1, e2) -> {
					return e1.getName().compareTo(e2.getName());
				});
		;
		enquiryFormsList.addComponentColumn(form -> {
			Link link = new Link();
			String linkURL = controller.getPublicEnquiryLink(form);
			link.setCaption(linkURL);
			link.setTargetName("_blank");
			link.setResource(new ExternalResource(linkURL));
			return link;
		}, msg.getMessage("EnquiryFormsComponent.linkCaption"), 20);

		enquiryFormsList.setItems(getEnquiryForms());

		VerticalLayout main = new VerticalLayout();
		Label enquiryCaption = new Label(msg.getMessage("EnquiryFormsComponent.caption"));
		enquiryCaption.setStyleName(Styles.sectionTitle.toString());
		main.addComponent(enquiryCaption);
		main.addComponent(buttonsBar);
		main.addComponent(enquiryFormsList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private List<SingleActionHandler<EnquiryForm>> getActionsHandlers()
	{
		SingleActionHandler<EnquiryForm> edit = SingleActionHandler.builder4Edit(msg, EnquiryForm.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		return Arrays.asList(edit);

	}

	private List<SingleActionHandler<EnquiryForm>> getHamburgerActionsHandlers()
	{

		SingleActionHandler<EnquiryForm> invite = SingleActionHandler.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("EnquiryFormsComponent.invite"))
				.withIcon(Images.envelope_open.getResource())
				.withHandler(items -> gotoInvitation(items.iterator().next())).build();

		SingleActionHandler<EnquiryForm> createRequest = SingleActionHandler.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("EnquiryFormsComponent.createResponse"))
				.withIcon(Images.file_add.getResource())
				.withHandler(items -> createResponse(items.iterator().next())).build();

		SingleActionHandler<EnquiryForm> clone = SingleActionHandler.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("EnquiryFormsComponent.clone"))
				.withIcon(Images.copy.getResource())
				.withHandler(items -> clone(items.iterator().next())).build();

		SingleActionHandler<EnquiryForm> remove = SingleActionHandler.builder4Delete(msg, EnquiryForm.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(invite, createRequest, clone, remove);

	}

	private void createResponse(EnquiryForm form)
	{
		adminEnquiryFormLauncher.showDialog(form, RemotelyAuthenticatedPrincipal.getLocalContext(),
				EnquiryFormsComponent.this::handleError);
	}

	private void handleError(Exception error)
	{
		NotificationPopup.showError(msg, msg.getMessage("EnquiryFormsComponent.errorShowFormEdit"), error);
	}

	private void clone(EnquiryForm form)
	{
		NavigationHelper.goToView(NewEnquiryFormView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ form.getName());
	}

	private void gotoInvitation(EnquiryForm form)
	{
		NavigationHelper.goToView(NewInvitationView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ form.getName() + "&" + CommonViewParam.type.toString() + "="
				+ InvitationType.ENQUIRY.toString());
	}

	private void gotoEdit(EnquiryForm form)
	{
		NavigationHelper.goToView(EditEnquiryFormView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ form.getName());
	}

	private Collection<EnquiryForm> getEnquiryForms()
	{
		try
		{
			return controller.getEnquiryForms();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private void remove(EnquiryForm form, boolean dropRequests)
	{
		try
		{
			controller.removeEnquiryForm(form, dropRequests);
			enquiryFormsList.removeElement(form);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(EnquiryForm form)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, Sets.newHashSet(form));

		new ConfirmWithOptionDialog(msg, msg.getMessage("EnquiryFormsComponent.confirmDelete", confirmText),
				msg.getMessage("EnquiryFormsComponent.dropRequests"),
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
