package econest.dao;

import java.util.List;

/**
 * Standard Create-Read-Update-Delete operations. Each entity DAO implements
 * this interface, so screens can treat them the same way (polymorphism).
 */
public interface CrudDao<T> {

    List<T> findAll();

    T findById(int id);

    /** Saves a new record and returns its generated id. */
    int insert(T entity);

    void update(T entity);

    void delete(int id);
}
