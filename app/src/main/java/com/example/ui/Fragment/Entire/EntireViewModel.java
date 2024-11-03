package com.example.ui.Fragment.Entire;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EntireViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public EntireViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is item1 fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}