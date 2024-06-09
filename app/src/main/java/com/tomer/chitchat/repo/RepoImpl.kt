package com.tomer.chitchat.repo

import com.tomer.chitchat.retro.Api
import javax.inject.Inject

class RepoImpl @Inject constructor( private val ret: Api) : MainRepo {
    override fun getData() {

    }
}