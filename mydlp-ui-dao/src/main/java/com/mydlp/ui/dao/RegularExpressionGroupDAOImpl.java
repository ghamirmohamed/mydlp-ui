package com.mydlp.ui.dao;

import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mydlp.ui.domain.RegularExpressionGroup;


@Repository("regularExpressionGroupDAO")
@Transactional
public class RegularExpressionGroupDAOImpl extends AbstractPolicyDAO implements RegularExpressionGroupDAO {

	@SuppressWarnings("unchecked")
	public List<RegularExpressionGroup> getRegularExpressionGroups() {
		DetachedCriteria criteria = 
				DetachedCriteria.forClass(RegularExpressionGroup.class);
		return getHibernateTemplate().findByCriteria(criteria);
	}

	@Override
	@Transactional(readOnly=false)
	public RegularExpressionGroup save(RegularExpressionGroup r) {
		getHibernateTemplate().saveOrUpdate(r);
		return r;
	}

}
