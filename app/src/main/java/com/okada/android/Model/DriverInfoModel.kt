package com.okada.android.Model

class DriverInfoModel {

    public var firstName = ""
    public var lastNmae = ""
    public var email = ""
    public var id = ""
    public var rating = 0.0
    public var uid = ""


    constructor(firstName: String, lastNmae: String, email: String,
                id: String, rating: Double) {
        this.firstName = firstName
        this.lastNmae = lastNmae
        this.email = email
        this.id = id
        this.rating = rating
    }



}