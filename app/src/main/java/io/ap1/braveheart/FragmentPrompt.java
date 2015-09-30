package io.ap1.braveheart;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Created by Tuotuo on 28/09/2015.
 */
public class FragmentPrompt extends DialogFragment implements View.OnClickListener{
    public FragmentPrompt() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view =  inflater.inflate(R.layout.fragment_prompt, container);

        view.findViewById(R.id.btn_signUp).setOnClickListener(this);
        view.findViewById(R.id.btn_shareToOthers).setOnClickListener(this);
        view.findViewById(R.id.btn_notInterested).setOnClickListener(this);
        view.findViewById(R.id.btn_showNextTime).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_signUp:
                //play video
                ((ActivityMain) getActivity()).signUp();
                dismiss();
                break;
            case R.id.btn_shareToOthers:
                //do survey
                ((ActivityMain) getActivity()).shareToOthers();
                dismiss();
                break;
            case R.id.btn_notInterested:
                //call url
                ((ActivityMain) getActivity()).notInterested();
                dismiss();
                break;
            case R.id.btn_showNextTime:
                ((ActivityMain) getActivity()).showNextTime();
                dismiss();
                break;
        }
    }
}
