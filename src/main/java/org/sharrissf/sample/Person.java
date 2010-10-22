package org.sharrissf.sample;

import java.io.Serializable;

public class Person implements Serializable {

    private final int age;
    private final String name;
    private final Gender gender;

    public Person(String name, int age, Gender gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public enum Gender {
        MALE, FEMALE;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name:" + name + ", age:" + age + ", sex:" + gender.name().toLowerCase() + ")";
    }

}