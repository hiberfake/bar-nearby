package de.piobyte.barnearby.data;

import java.io.Serializable;
import java.util.Map;

public class Menu implements Serializable {

    private Map<String, Float> drinks;
    private Map<String, Float> meals;

    public Map<String, Float> getDrinks() {
        return drinks;
    }

    public Map<String, Float> getMeals() {
        return meals;
    }
}