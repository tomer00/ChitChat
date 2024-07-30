package com.tomer.chitchat.viewmodals

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.repo.RepoPersons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModal @Inject constructor(
    private  val repoPersons: RepoPersons
) : ViewModel (){

    private val _persons = MutableLiveData<List<PersonModel>>()
    val persons : LiveData<List<PersonModel>> = _persons

    fun loadPersons(){
        viewModelScope.launch {
            val per = repoPersons.getAllPersons()
            Log.d("TAG--", "loadPersons: $per")
            _persons.postValue(per)
        }
    }
}