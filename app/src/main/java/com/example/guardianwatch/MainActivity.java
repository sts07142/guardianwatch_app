package com.example.guardianwatch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    View childListView;
    View alarmView;
    View activityAmountView;
    View activityRecordView;
    ViewPager2 viewPager2;
    ImageView leftArrow, rightArrow;
    List<SliderItem> sliderItems = new ArrayList<>();
    private int mainPageKid_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager2 = findViewById(R.id.viewPager);

        mainPageKid_index = viewPager2.getCurrentItem();

        leftArrow = findViewById(R.id.leftArrow);
        rightArrow = findViewById(R.id.rightArrow);

        leftArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager2.getCurrentItem();
                if (currentItem > 0) {
                    viewPager2.setCurrentItem(currentItem - 1);
                    mainPageKid_index = viewPager2.getCurrentItem();
                }
            }
        });

        rightArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager2.getCurrentItem();
                if (currentItem < sliderItems.size() - 1) { // sliderItems는 ViewPager의 항목 수를 가져오는 코드로 수정해야 합니다.
                    viewPager2.setCurrentItem(currentItem + 1);
                    mainPageKid_index = viewPager2.getCurrentItem();
                }
            }
        });
        //아이 목록 누를시에 아이 목록 페이지로 이동
        childListView = findViewById(R.id.childListView);
        alarmView = findViewById(R.id.alarmView);
        activityAmountView = findViewById(R.id.activityAmountView);
        activityRecordView = findViewById(R.id.activityRecordView);

        childListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChildListActivity.class);
                startActivity(intent);

            }
        });
        alarmView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), NotificationListActivity.class);
                startActivity(intent);
            }
        });
        activityAmountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d("Main Selected Kid", UserData.getInstance().getUserId());
//                Log.d("Main Selected Kid", String.valueOf(mainPageKid_index));
                SliderItem currentItem = sliderItems.get(mainPageKid_index);
//                Log.d("Main Selected Kid", currentItem.getText());
                Intent intent = new Intent(getApplicationContext(), GraphActivity.class);
                intent.putExtra("userId", UserData.getInstance().getUserId());
                intent.putExtra("kidName", currentItem.getText());
                startActivity(intent);
            }
        });
        activityRecordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SliderItem currentItem = sliderItems.get(mainPageKid_index);
                Intent intent = new Intent(getApplicationContext(), BevActivity.class);
                intent.putExtra("userId", UserData.getInstance().getUserId());
                intent.putExtra("kidName", currentItem.getText());
                startActivity(intent);

            }
        });
//        ViewPager2 viewPager2 = findViewById(R.id.viewPager);
//
//        List<SliderItem> sliderItems = Arrays.asList(
//                new SliderItem("URL_1", "이름1"),
//                new SliderItem("URL_2", "이름2"),
//                new SliderItem("URL_3", "이름3")
//        );
//        SliderAdapter adapter = new SliderAdapter(sliderItems);
//        viewPager2.setAdapter(adapter);
        fetchKidsDataAndSetupViewPager(UserData.getInstance().getUserId());

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isRepresentChanged = sharedPreferences.getBoolean("represent_changed", false);

        if (isRepresentChanged) {
            // 대표 아이가 변경되었으므로 ViewPager2의 아이 데이터를 새로고침합니다.
            fetchKidsDataAndSetupViewPager(UserData.getInstance().getUserId());

            // SharedPreferences의 값을 다시 false로 설정하여 플래그를 초기화합니다.
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("represent_changed", false);
            editor.apply();
        }
    }

    private void fetchKidsDataAndSetupViewPager(String userId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://210.102.178.157:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service service = retrofit.create(Service.class);
        Call<ResponseBody> call = service.getKids(userId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {

                        String responseData = response.body().string();
                        Log.d("MAIN_ACTIVITY", "Response received: " + responseData);

                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<ChildData>>() {
                        }.getType();
                        List<ChildData> kidsList = gson.fromJson(responseData, listType);

                        SliderItem representChild = null;  // 대표 아이를 저장할 변수

                        for (ChildData child : kidsList) {
                            SliderItem item = new SliderItem("DEFAULT_PROFILE_IMAGE", child.getName(), child.getRepresent());
                            if (child.getRepresent() == 1) {
                                representChild = item;  // 대표 아이를 저장
                            } else {
                                sliderItems.add(item);
                            }
                        }
                        // 대표 아이가 있으면 맨 앞에 추가
                        if (representChild != null) {
                            sliderItems.add(0, representChild);
                        }

                        SliderAdapter adapter = new SliderAdapter(sliderItems);
                        ViewPager2 viewPager2 = findViewById(R.id.viewPager);
                        viewPager2.setAdapter(adapter);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Handle the error
            }
        });
    }

    //메인 페이지에서 뒤로가기 누를 시에 앱 종료 팝업 화면 띄우기
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("앱을 종료하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();  // 현재 액티비티와 같은 taskAffinity를 가진 모든 액티비티를 종료합니다.
                        System.exit(0);  // 시스템을 통해 앱의 프로세스를 종료합니다.
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }


}