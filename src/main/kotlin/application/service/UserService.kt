package com.castle.application.service

class UserService {
    private val userId: String = "123"

    fun existsUserById(id: String): Boolean = id == userId
}
