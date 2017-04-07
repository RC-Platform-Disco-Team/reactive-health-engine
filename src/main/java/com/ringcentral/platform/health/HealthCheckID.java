package com.ringcentral.platform.health;

public class HealthCheckID implements Comparable<HealthCheckID> {

    private final String shortName;

    private final String description;

    public HealthCheckID(String shortName) {
        this(shortName, shortName);
    }

    public HealthCheckID(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HealthCheckID that = (HealthCheckID) o;
        return shortName.equals(that.shortName);
    }

    @Override
    public int hashCode() {
        return shortName.hashCode();
    }

    @Override
    public String toString() {
        return shortName;
    }

    @Override
    public int compareTo(HealthCheckID o) {
        return shortName.compareTo(o.shortName);
    }
}
