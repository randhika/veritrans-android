package id.co.veritrans.sdk.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import id.co.veritrans.sdk.R;
import id.co.veritrans.sdk.core.LocalDataHandler;
import id.co.veritrans.sdk.core.StorageDataHandler;
import id.co.veritrans.sdk.models.UserDetail;

public class InstructionIndosatFragment extends Fragment {

    private EditText mEditTextPhoneNumber = null;
    private UserDetail userDetail;
    private StorageDataHandler storageDataHandler;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_instruction_indosat, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        try {
            userDetail = LocalDataHandler.readObject(getString(R.string.user_details), UserDetail.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mEditTextPhoneNumber = (EditText) view.findViewById(R.id.et_indosat_phone_number);
        if(!TextUtils.isEmpty(userDetail.getPhoneNumber())) {
            mEditTextPhoneNumber.setText(userDetail.getPhoneNumber());
        }
    }

    public String getPhoneNumber() {
        if (mEditTextPhoneNumber != null) {
            return mEditTextPhoneNumber.getText().toString();
        } else {
            return null;
        }
    }

}
