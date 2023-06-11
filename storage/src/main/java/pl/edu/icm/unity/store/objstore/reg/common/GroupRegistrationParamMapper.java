/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam.IncludeGroupsMode;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;

class GroupRegistrationParamMapper
{
	static DBGroupRegistrationParam map(GroupRegistrationParam groupRegistrationParam)
	{
		return DBGroupRegistrationParam.builder()
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

	static GroupRegistrationParam map(DBGroupRegistrationParam restGroupRegistrationParam)
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
