package com.library.common;

import java.util.List;

public interface Repository<T> {
    void save(T obj);
    List<T> getAll();
    void delete(int id);
}
