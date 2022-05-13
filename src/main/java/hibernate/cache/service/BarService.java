package hibernate.cache.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hibernate.cache.dao.IBarDao;
import hibernate.cache.model.Bar;

@Service
@Transactional
public class BarService {

    @Autowired
    private IBarDao dao;

    public BarService() {
        super();
    }

    // API

    public void create(final Bar entity) {
        dao.create(entity);
    }

    public Bar findOne(final long id) {
        return dao.findOne(id);
    }

    public List<Bar> findAll() {
        return dao.findAll();
    }

    public void update(final Bar entity) {
    	dao.update(entity);
    }
}