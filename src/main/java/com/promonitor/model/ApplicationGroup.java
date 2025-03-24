package com.promonitor.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ApplicationGroup {
    private final StringProperty name = new SimpleStringProperty();
    private final ObservableList<Application> applications = FXCollections.observableArrayList();

    public ApplicationGroup(String name) {
        this.name.set(name);
    }

    public boolean addApplication(Application app) {
        if (!applications.contains(app)) {
            return applications.add(app);
        }
        return false;
    }


    public boolean removeApplication(Application app) {
        return applications.remove(app);
    }

    public boolean containsApplication(Application app) {
        return applications.contains(app);
    }

    public String getName() {
        return name.get();
    }

    public ObservableList<Application> getApplications() {
        return applications;
    }

    public int getApplicationCount() {
        return applications.size();
    }

    @Override
    public String toString() {
        return name.get() + " (" + applications.size() + " ứng dụng)";
    }
}