package com.example.mvvm.viewmodals

import androidx.lifecycle.ViewModel
import com.example.mvvm.repo.RepoImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MainViewModal @Inject constructor(private  val repo:RepoImpl) : ViewModel (){

}