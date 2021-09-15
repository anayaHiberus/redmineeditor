package com.hiberus.anaya.redmineeditor.controllers;

public abstract class InnerCtrl {
    protected MainCtrl mainCtrl;

    protected void setMainController(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

}
