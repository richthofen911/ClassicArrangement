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

        view.findViewById(R.id.btn_pullVideo).setOnClickListener(this);
        view.findViewById(R.id.btn_doSurvey).setOnClickListener(this);
        view.findViewById(R.id.btn_justPerk).setOnClickListener(this);
        view.findViewById(R.id.btn_cancelPrompt).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_pullVideo:
                //play video
                ((ActivityMain) getActivity()).pullVideo();
                dismiss();
                break;
            case R.id.btn_doSurvey:
                //do survey
                ((ActivityMain) getActivity()).doSurvey();
                dismiss();
                break;
            case R.id.btn_justPerk:
                //call url
                ((ActivityMain) getActivity()).getPerk();
                dismiss();
                break;
            case R.id.btn_cancelPrompt:
                ((ActivityMain) getActivity()).cancelPrompt();
                dismiss();
                break;
        }
    }
}
