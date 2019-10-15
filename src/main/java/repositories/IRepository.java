package repositories;

import java.util.ArrayList;

public interface IRepository<T> {
    ArrayList<T> getAll();

    boolean addToRepo(String input);

    void deleteItem();

    T getItem(int projectNumber);
}
