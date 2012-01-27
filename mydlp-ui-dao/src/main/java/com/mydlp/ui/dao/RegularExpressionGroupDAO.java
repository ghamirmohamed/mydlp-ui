package com.mydlp.ui.dao;

import java.util.List;

import com.mydlp.ui.domain.RegularExpressionGroup;


public interface RegularExpressionGroupDAO {
	
	 public List<RegularExpressionGroup> getRegularExpressionGroups();
	 
	 public RegularExpressionGroup save(RegularExpressionGroup r);
}
