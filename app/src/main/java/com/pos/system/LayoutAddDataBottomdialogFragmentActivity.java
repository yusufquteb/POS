package com.pos.system;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.multidex.*;
import androidx.preference.*;
import androidx.work.*;
import com.android.billingclient.*;
import com.bumptech.glide.*;
import com.dantsu.escposprinter.*;
import com.github.mikephil.charting.*;
import com.google.android.material.*;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.play.review.*;
import com.google.zxing.client.android.*;
import com.pos.system.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class LayoutAddDataBottomdialogFragmentActivity extends BottomSheetDialogFragment {
	
	private LayoutAddDataBottomdialogFragmentBinding binding;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		binding = LayoutAddDataBottomdialogFragmentBinding.inflate(_inflater, _container, false);
		initialize(_savedInstanceState, binding.getRoot());
		initializeLogic();
		return binding.getRoot();
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
	}
	
	private void initializeLogic() {
	}
	
}
