package com.okada.android.Model

class DriverInfoModel {

    private var firstName = ""
    private var lastNmae = ""
    private var email = ""
    private var id = ""
    private var rating = 0.0


    constructor(firstName: String, lastNmae: String, email: String, id: String, rating: Double) {
        this.firstName = firstName
        this.lastNmae = lastNmae
        this.email = email
        this.id = id
        this.rating = rating
    }

}