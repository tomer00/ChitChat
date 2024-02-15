package com.example.mvvm.repo

import com.example.mvvm.retro.Api
import javax.inject.Inject

class RepoImpl @Inject constructor( private val ret: Api) : MainRepo {
    override fun getData() {

    }
}