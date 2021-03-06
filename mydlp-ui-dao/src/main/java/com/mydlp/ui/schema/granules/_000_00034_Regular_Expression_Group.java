package com.mydlp.ui.schema.granules;

import java.util.ArrayList;
import java.util.List;

import com.mydlp.ui.domain.RegularExpressionGroup;
import com.mydlp.ui.domain.RegularExpressionGroupEntry;
import com.mydlp.ui.schema.AbstractGranule;

public class _000_00034_Regular_Expression_Group extends AbstractGranule {

	
	public RegularExpressionGroupEntry createMTObj(String regex) {
		RegularExpressionGroupEntry rege = new RegularExpressionGroupEntry();
		rege.setRegex(regex);
		getHibernateTemplate().saveOrUpdate(rege);
		return rege;
	}
	
	@Override
	protected void callback() {
		RegularExpressionGroup reg = new RegularExpressionGroup();
		reg.setNameKey("regularExpression.testSecond.label");
		getHibernateTemplate().saveOrUpdate(reg);
		//df.setMatcher(m);
		List<RegularExpressionGroupEntry> reges = new ArrayList<RegularExpressionGroupEntry>();
		reges.add(createMTObj("example/keyword/4"));
		reges.add(createMTObj("example/keyword/5"));
		reges.add(createMTObj("example/keyword/6"));
		reg.setEntries(reges);
		getHibernateTemplate().saveOrUpdate(reg);
	}
}
