/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.idpStatistics;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

public interface IdpStatisticMapper extends BasicCRUDMapper<IdpStatisticBean>
{
	List<IdpStatisticBean> getStatistics(@Param("from") Date from, @Param("until") Date until,
			@Param("limit") int limit);
	
	void deleteOlderThan(@Param("olderThan") Date olderThan);
}
