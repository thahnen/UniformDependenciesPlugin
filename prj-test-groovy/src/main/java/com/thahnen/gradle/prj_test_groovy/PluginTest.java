package com.thahnen.gradle.prj_test_groovy;

import com.google.gson.Gson;


/**
 *  Simple class to show off Gradle plugin ;)
 *
 *  @author thahnen
 */
public class PluginTest {
    /// main function
    public static void main(String[] args) {
        Gson gson = new Gson();

        Developer thahnen = new Developer(
                "Tobias Hahnen",
                22,
                0   // He only drinks tea!
        );

        String bestDeveloper = gson.toJson(thahnen);
        System.out.print("Best developer as JSON: " + bestDeveloper);
    }


    /**
     *  Class to represent a developer
     */
    static class Developer {
        String name;
        int age;
        int coffeeCount;

        /// constructor
        Developer(String nName, int nAge, int nCoffeeCount) {
            name = nName;
            age = nAge;
            coffeeCount = nCoffeeCount;
        }
    }
}
