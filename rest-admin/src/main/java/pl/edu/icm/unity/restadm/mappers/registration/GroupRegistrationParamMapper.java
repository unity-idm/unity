/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestGroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam.IncludeGroupsMode;

public class GroupRegistrationParamMapper
{
	public static RestGroupRegistrationParam map(GroupRegistrationParam groupRegistrationParam)
	{
		return RestGroupRegistrationParam.builder()
				.withGroupPath(groupRegistrationParam.getGroupPath())
				.withIncludeGroupsMode(Optional.ofNullable(groupRegistrationParam.getIncludeGroupsMode())
						.map(IncludeGroupsMode::name)
						.orElse(null))
				.withMultiSelect(groupRegistrationParam.isMultiSelect())
				.withDescription(groupRegistrationParam.getDescription())
				.withLabel(groupRegistrationParam.getLabel())
				.withRetrievalSettings(groupRegistrationParam.getRetrievalSettings()
						.name())
				.build();
	}

	public static GroupRegistrationParam map(RestGroupRegistrationParam restGroupRegistrationParam)
	{
		GroupRegistrationParam groupRegistrationParam = new GroupRegistrationParam();
		groupRegistrationParam.setGroupPath(restGroupRegistrationParam.groupPath);
		groupRegistrationParam.setIncludeGroupsMode(Optional.ofNullable(restGroupRegistrationParam.includeGroupsMode)
				.map(IncludeGroupsMode::valueOf)
				.orElse(null));
		groupRegistrationParam.setMultiSelect(restGroupRegistrationParam.multiSelect);
		groupRegistrationParam.setDescription(restGroupRegistrationParam.description);
		groupRegistrationParam.setLabel(restGroupRegistrationParam.label);
		groupRegistrationParam
				.setRetrievalSettings(ParameterRetrievalSettings.valueOf(restGroupRegistrationParam.retrievalSettings));
		return groupRegistrationParam;
	}
}
