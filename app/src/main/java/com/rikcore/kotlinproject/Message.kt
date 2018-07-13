package com.rikcore.kotlinproject

class Message {

    var sender : String? = null
    var senderId : String? = null
    var receiver : String? = null
    var receiverId : String? = null
    var content : String? = null
    var timeStamp : Long? = null

    constructor(){
        //Empty for Firebase
    }
    constructor(sender : String, senderId : String, receiver : String, receiverId : String, content : String, timeStamp : Long) {
        this.sender = sender
        this.senderId = senderId
        this.receiver = receiver
        this.receiverId = receiverId
        this.content = content
        this.timeStamp = timeStamp
    }

}